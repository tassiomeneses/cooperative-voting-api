package br.com.cooperativevoting.voting.adapter.out.persistence;

import br.com.cooperativevoting.voting.adapter.out.persistence.entity.AgendaJpaEntity;
import br.com.cooperativevoting.voting.adapter.out.persistence.entity.VotingSessionJpaEntity;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.AgendaJpaMapper;
import br.com.cooperativevoting.voting.adapter.out.persistence.repository.SpringDataAgendaRepository;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAgendaRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Mock
    private SpringDataAgendaRepository repository;

    private JpaAgendaRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAgendaRepositoryAdapter(repository, new AgendaJpaMapper());
    }

    @Test
    void shouldRejectSessionSaveWhenAgendaAlreadyHasAnotherSession() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession newSession = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        Agenda agendaWithNewSession = agenda.withVotingSession(newSession);

        AgendaJpaEntity persistedAgenda = new AgendaJpaEntity(
            agenda.getId().value(),
            agenda.getTitle().value(),
            agenda.getDescription().value(),
            agenda.getCreatedAt()
        );
        persistedAgenda.attachVotingSession(new VotingSessionJpaEntity(
            UUID.randomUUID(),
            NOW.minusSeconds(120),
            NOW.minusSeconds(60)
        ));

        when(repository.findByIdWithVotingSession(agenda.getId().value())).thenReturn(Optional.of(persistedAgenda));

        assertThrows(VotingSessionAlreadyOpenedException.class, () -> adapter.save(agendaWithNewSession));
    }

    @Test
    void shouldTranslateUniqueConstraintViolationToSessionAlreadyOpened() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        Agenda agendaWithSession = agenda.withVotingSession(session);

        when(repository.findByIdWithVotingSession(agenda.getId().value())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any()))
            .thenThrow(new DataIntegrityViolationException("violates constraint uk_voting_sessions_agenda_id"));

        assertThrows(VotingSessionAlreadyOpenedException.class, () -> adapter.save(agendaWithSession));
    }
}
