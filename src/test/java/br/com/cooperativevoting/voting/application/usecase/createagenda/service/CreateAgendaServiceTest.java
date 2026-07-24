package br.com.cooperativevoting.voting.application.usecase.createagenda.service;

import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaInput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.mapper.CreateAgendaMapper;
import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAgendaServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Mock
    private AgendaRepositoryPort agendaRepositoryPort;

    private CreateAgendaService service;

    @BeforeEach
    void setUp() {
        service = new CreateAgendaService(
            agendaRepositoryPort,
            new CreateAgendaMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateAgenda() {
        when(agendaRepositoryPort.save(any(Agenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAgendaOutput output = service.execute(
            new CreateAgendaInput("Approve annual report", "Annual cooperative report")
        );

        assertEquals("Approve annual report", output.title());
        assertEquals("Annual cooperative report", output.description());
        assertEquals(NOW, output.createdAt());
        verify(agendaRepositoryPort).save(any(Agenda.class));
    }

    @Test
    void shouldCreateAgendaWithoutVotingSession() {
        when(agendaRepositoryPort.save(any(Agenda.class))).thenAnswer(invocation -> {
            Agenda agenda = invocation.getArgument(0);
            assertFalse(agenda.currentSession().isPresent());
            return agenda;
        });

        service.execute(new CreateAgendaInput("Approve annual report", null));

        verify(agendaRepositoryPort).save(any(Agenda.class));
    }

    @Test
    void shouldRejectInvalidTitle() {
        assertThrows(
            InvalidDomainObjectException.class,
            () -> service.execute(new CreateAgendaInput(" ", "Invalid agenda"))
        );
    }
}
