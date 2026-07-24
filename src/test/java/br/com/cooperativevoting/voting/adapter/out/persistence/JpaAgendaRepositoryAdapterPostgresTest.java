package br.com.cooperativevoting.voting.adapter.out.persistence;

import br.com.cooperativevoting.testsupport.PostgreSqlContainerSupport;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.AgendaJpaMapper;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAgendaRepositoryAdapter.class, AgendaJpaMapper.class})
class JpaAgendaRepositoryAdapterPostgresTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Autowired
    private JpaAgendaRepositoryAdapter adapter;

    @Test
    void shouldPersistAndFindAgendaWithVotingSession() {
        Agenda agenda = AgendaFactory.create("Approve annual report", "Annual report", NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);

        adapter.save(agenda);
        adapter.save(agenda.withVotingSession(session));

        Agenda foundAgenda = adapter.findById(agenda.getId()).orElseThrow();

        assertEquals(agenda.getId(), foundAgenda.getId());
        assertEquals("Approve annual report", foundAgenda.getTitle().value());
        assertTrue(foundAgenda.currentSession().isPresent());
        assertEquals(session.getClosedAt(), foundAgenda.currentSession().orElseThrow().getClosedAt());
    }

    @Test
    void shouldReturnEmptyWhenAgendaDoesNotExist() {
        assertTrue(adapter.findById(AgendaId.newId()).isEmpty());
    }

    @Test
    void shouldRejectSecondSessionForSameAgenda() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession firstSession = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        VotingSession secondSession = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW.plusSeconds(1));

        adapter.save(agenda);
        adapter.save(agenda.withVotingSession(firstSession));

        Agenda staleAgendaWithoutSession = Agenda.restore(
            agenda.getId(),
            agenda.getTitle(),
            agenda.getDescription(),
            agenda.getCreatedAt(),
            null
        );

        assertThrows(
            VotingSessionAlreadyOpenedException.class,
            () -> adapter.save(staleAgendaWithoutSession.withVotingSession(secondSession))
        );
    }
}
