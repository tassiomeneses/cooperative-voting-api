package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.factory.VoteFactory;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteFactoryTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void shouldCastVoteWhenAgendaSessionIsOpen() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        Agenda agendaWithSession = agenda.withVotingSession(session);

        Vote vote = VoteFactory.cast(
            agendaWithSession,
            AssociateId.from("associate-1"),
            VoteChoice.YES,
            NOW.plusSeconds(30)
        );

        assertEquals(agenda.getId(), vote.getAgendaId());
        assertEquals("associate-1", vote.getAssociateId().value());
        assertTrue(vote.isYes());
    }

    @Test
    void shouldRejectVoteWhenAgendaHasNoOpenSession() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);

        assertThrows(
            VotingSessionClosedException.class,
            () -> VoteFactory.cast(agenda, AssociateId.from("associate-1"), VoteChoice.YES, NOW)
        );
    }

    @Test
    void shouldRejectVoteWhenSessionIsExpired() {
        Agenda agenda = AgendaFactory.create("Approve annual report", null, NOW);
        VotingSession session = VotingSessionFactory.openFor(agenda, VotingSessionDuration.DEFAULT, NOW);
        Agenda agendaWithSession = agenda.withVotingSession(session);

        assertThrows(
            VotingSessionClosedException.class,
            () -> VoteFactory.cast(agendaWithSession, AssociateId.from("associate-1"), VoteChoice.YES, NOW.plusSeconds(60))
        );
    }
}
