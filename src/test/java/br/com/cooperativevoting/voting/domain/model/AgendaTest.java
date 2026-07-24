package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void shouldCreateAgendaWithoutVotingSession() {
        Agenda agenda = AgendaFactory.create("Approve annual report", "Annual cooperative report", NOW);

        assertEquals("Approve annual report", agenda.getTitle().value());
        assertTrue(agenda.currentSession().isEmpty());
    }

    @Test
    void shouldAttachVotingSessionToAgenda() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);

        Agenda agendaWithSession = agenda.withVotingSession(session);

        assertTrue(agendaWithSession.currentSession().isPresent());
        assertTrue(agendaWithSession.hasOpenSessionAt(NOW.plusSeconds(30)));
    }

    @Test
    void shouldRejectSecondVotingSessionForSameAgenda() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        Agenda agendaWithSession = agenda.withVotingSession(session);
        VotingSession secondSession = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW.plusSeconds(120));

        assertThrows(
            VotingSessionAlreadyOpenedException.class,
            () -> agendaWithSession.withVotingSession(secondSession)
        );
    }

    @Test
    void shouldRejectVotingSessionFromAnotherAgenda() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        Agenda anotherAgenda = AgendaFactory.create("Approve budget", null, NOW);
        VotingSession anotherSession = VotingSessionFactory.openFor(anotherAgenda, VotingSessionDuration.DEFAULT, NOW);

        assertThrows(InvalidDomainObjectException.class, () -> agenda.withVotingSession(anotherSession));
    }
}
