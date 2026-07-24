package br.com.cooperativevoting.voting.application.usecase.openvotingsession.service;

import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.mapper.OpenVotingSessionMapper;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenVotingSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Mock
    private AgendaRepositoryPort agendaRepositoryPort;

    private OpenVotingSessionService service;

    @BeforeEach
    void setUp() {
        service = new OpenVotingSessionService(
            agendaRepositoryPort,
            new OpenVotingSessionMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldOpenVotingSessionWithDefaultDuration() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        when(agendaRepositoryPort.findById(agenda.getId())).thenReturn(Optional.of(agenda));
        when(agendaRepositoryPort.save(any(Agenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpenVotingSessionOutput output = service.execute(
            new OpenVotingSessionInput(agenda.getId().toString(), null)
        );

        assertEquals(agenda.getId().toString(), output.agendaId());
        assertEquals(NOW, output.openedAt());
        assertEquals(NOW.plusSeconds(60), output.closedAt());
        assertEquals(VotingSessionStatus.OPEN, output.status());
        verify(agendaRepositoryPort).save(any(Agenda.class));
    }

    @Test
    void shouldOpenVotingSessionWithCustomDuration() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        when(agendaRepositoryPort.findById(agenda.getId())).thenReturn(Optional.of(agenda));
        when(agendaRepositoryPort.save(any(Agenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpenVotingSessionOutput output = service.execute(
            new OpenVotingSessionInput(agenda.getId().toString(), 5L)
        );

        assertEquals(NOW.plusSeconds(300), output.closedAt());
    }

    @Test
    void shouldRejectWhenAgendaDoesNotExist() {
        AgendaId agendaId = AgendaId.newId();
        when(agendaRepositoryPort.findById(agendaId)).thenReturn(Optional.empty());

        assertThrows(
            AgendaNotFoundException.class,
            () -> service.execute(new OpenVotingSessionInput(agendaId.toString(), null))
        );
        verify(agendaRepositoryPort, never()).save(any(Agenda.class));
    }

    @Test
    void shouldRejectWhenAgendaAlreadyHasVotingSession() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        Agenda agendaWithSession = agenda.withVotingSession(session);
        when(agendaRepositoryPort.findById(agenda.getId())).thenReturn(Optional.of(agendaWithSession));

        assertThrows(
            VotingSessionAlreadyOpenedException.class,
            () -> service.execute(new OpenVotingSessionInput(agenda.getId().toString(), null))
        );
        verify(agendaRepositoryPort, never()).save(any(Agenda.class));
    }
}
