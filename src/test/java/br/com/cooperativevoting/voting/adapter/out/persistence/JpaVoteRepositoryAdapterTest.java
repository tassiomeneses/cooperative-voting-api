package br.com.cooperativevoting.voting.adapter.out.persistence;

import br.com.cooperativevoting.shared.security.CpfHashingService;
import br.com.cooperativevoting.shared.security.IdentityHashProperties;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.VoteJpaMapper;
import br.com.cooperativevoting.voting.adapter.out.persistence.repository.SpringDataVoteRepository;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.exception.DuplicateVoteException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import br.com.cooperativevoting.voting.domain.model.vo.VoteId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaVoteRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final Cpf CPF = Cpf.from("529.982.247-25");

    @Mock
    private SpringDataVoteRepository repository;

    private JpaVoteRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaVoteRepositoryAdapter(
            repository,
            new VoteJpaMapper(),
            new CpfHashingService(new IdentityHashProperties("test-pepper", false))
        );
    }

    @Test
    void shouldTranslateUniqueConstraintViolationToDuplicateVote() {
        Vote vote = Vote.cast(
            VoteId.newId(),
            AgendaId.newId(),
            AssociateId.from("associate-1"),
            VoteChoice.YES,
            NOW
        );
        when(repository.insertIfSessionOpen(any(), any(), any(), any(), any(), any()))
            .thenThrow(new DataIntegrityViolationException("violates constraint uk_votes_agenda_voter_key"));

        assertThrows(DuplicateVoteException.class, () -> adapter.saveIfSessionOpen(vote, CPF));
    }

    @Test
    void shouldRejectWhenDatabaseDoesNotFindOpenSessionForVote() {
        Vote vote = Vote.cast(
            VoteId.newId(),
            AgendaId.newId(),
            AssociateId.from("associate-1"),
            VoteChoice.YES,
            NOW
        );
        when(repository.insertIfSessionOpen(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(repository.existsAgendaById(vote.getAgendaId().value())).thenReturn(true);

        assertThrows(VotingSessionClosedException.class, () -> adapter.saveIfSessionOpen(vote, CPF));
    }

    @Test
    void shouldRejectWhenAgendaDoesNotExist() {
        Vote vote = Vote.cast(
            VoteId.newId(),
            AgendaId.newId(),
            AssociateId.from("associate-1"),
            VoteChoice.YES,
            NOW
        );
        when(repository.insertIfSessionOpen(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(repository.existsAgendaById(vote.getAgendaId().value())).thenReturn(false);

        assertThrows(AgendaNotFoundException.class, () -> adapter.saveIfSessionOpen(vote, CPF));
    }
}
