package br.com.cooperativevoting.voting.adapter.out.persistence;

import br.com.cooperativevoting.shared.security.CpfHashingService;
import br.com.cooperativevoting.shared.security.IdentityHashConfiguration;
import br.com.cooperativevoting.testsupport.PostgreSqlContainerSupport;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.AgendaJpaMapper;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.VoteJpaMapper;
import br.com.cooperativevoting.voting.application.port.out.VoteTally;
import br.com.cooperativevoting.voting.domain.exception.DuplicateVoteException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import br.com.cooperativevoting.voting.domain.model.vo.VoteId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaAgendaRepositoryAdapter.class,
    JpaVoteRepositoryAdapter.class,
    AgendaJpaMapper.class,
    VoteJpaMapper.class,
    CpfHashingService.class,
    IdentityHashConfiguration.class
})
class JpaVoteRepositoryAdapterPostgresTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final Cpf CPF_1 = Cpf.from("529.982.247-25");
    private static final Cpf CPF_2 = Cpf.from("111.444.777-35");
    private static final Cpf CPF_3 = Cpf.from("935.411.347-80");

    @Autowired
    private JpaAgendaRepositoryAdapter agendaRepositoryAdapter;

    @Autowired
    private JpaVoteRepositoryAdapter voteRepositoryAdapter;

    @Test
    void shouldPersistVotesAndUpdateAgendaTotalBuckets() {
        Agenda agenda = agendaRepositoryAdapter.save(openAgendaAt(NOW.minusSeconds(30), VotingSessionDuration.fromMinutes(5)));

        voteRepositoryAdapter.saveIfSessionOpen(voteFor(agenda, "associate-1", VoteChoice.YES), CPF_1);
        voteRepositoryAdapter.saveIfSessionOpen(voteFor(agenda, "associate-2", VoteChoice.YES), CPF_2);
        voteRepositoryAdapter.saveIfSessionOpen(voteFor(agenda, "associate-3", VoteChoice.NO), CPF_3);

        VoteTally tally = agendaRepositoryAdapter.findVoteTallyById(agenda.getId()).orElseThrow();

        assertEquals(2, tally.yesVotes());
        assertEquals(1, tally.noVotes());
    }

    @Test
    void shouldRejectDuplicatedVoteByCpfEvenWhenAssociateIdIsDifferent() {
        Agenda agenda = agendaRepositoryAdapter.save(openAgendaAt(NOW.minusSeconds(30), VotingSessionDuration.fromMinutes(5)));
        voteRepositoryAdapter.saveIfSessionOpen(voteFor(agenda, "associate-1", VoteChoice.YES), CPF_1);

        Vote duplicatedVote = voteFor(agenda, "associate-2", VoteChoice.NO);

        assertThrows(DuplicateVoteException.class, () -> voteRepositoryAdapter.saveIfSessionOpen(duplicatedVote, CPF_1));
    }

    @Test
    void shouldRejectVoteWhenDatabaseSessionWindowIsClosed() {
        Agenda agenda = agendaRepositoryAdapter.save(openAgendaAt(NOW.minusSeconds(120), VotingSessionDuration.DEFAULT));
        Vote staleVote = voteFor(agenda, "associate-1", VoteChoice.YES);

        assertThrows(VotingSessionClosedException.class, () -> voteRepositoryAdapter.saveIfSessionOpen(staleVote, CPF_1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldAllowOnlyOneVoteForSameCpfUnderConcurrency() throws Exception {
        Agenda agenda = agendaRepositoryAdapter.save(openAgendaAt(NOW.minusSeconds(30), VotingSessionDuration.fromMinutes(5)));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> firstVote = concurrentVote(agenda, "associate-1", VoteChoice.YES, CPF_1, ready, start);
        Callable<Boolean> secondVote = concurrentVote(agenda, "associate-2", VoteChoice.NO, CPF_1, ready, start);

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(firstVote);
            var second = executor.submit(secondVote);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<Boolean> results = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
            VoteTally tally = agendaRepositoryAdapter.findVoteTallyById(agenda.getId()).orElseThrow();
            assertEquals(1, tally.yesVotes() + tally.noVotes());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Vote voteFor(Agenda agenda, String associateId, VoteChoice choice) {
        return Vote.cast(
            VoteId.newId(),
            agenda.getId(),
            AssociateId.from(associateId),
            choice,
            NOW
        );
    }

    private Callable<Boolean> concurrentVote(
        Agenda agenda,
        String associateId,
        VoteChoice choice,
        Cpf cpf,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            assertTrue(start.await(5, TimeUnit.SECONDS));
            try {
                voteRepositoryAdapter.saveIfSessionOpen(voteFor(agenda, associateId, choice), cpf);
                return true;
            } catch (DuplicateVoteException exception) {
                return false;
            }
        };
    }

    private static Agenda openAgendaAt(Instant openedAt, VotingSessionDuration duration) {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, openedAt);
        return agenda.withVotingSession(VotingSessionFactory.openFor(agenda, duration, openedAt));
    }
}
