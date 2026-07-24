package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VotingSessionTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void shouldOpenSessionWithDefaultDurationWhenDurationIsNull() {
        VotingSession session = VotingSession.open(
            VotingSessionId.newId(),
            AgendaId.newId(),
            null,
            NOW
        );

        assertEquals(NOW.plusSeconds(60), session.getClosedAt());
        assertEquals(VotingSessionStatus.OPEN, session.statusAt(NOW.plusSeconds(30)));
    }

    @Test
    void shouldBeClosedAtExactClosingInstant() {
        VotingSession session = VotingSession.open(
            VotingSessionId.newId(),
            AgendaId.newId(),
            VotingSessionDuration.from(Duration.ofSeconds(10)),
            NOW
        );

        assertEquals(VotingSessionStatus.CLOSED, session.statusAt(NOW.plusSeconds(10)));
    }

    @Test
    void shouldRejectInvalidClosingDate() {
        assertThrows(
            InvalidDomainObjectException.class,
            () -> VotingSession.restore(VotingSessionId.newId(), AgendaId.newId(), NOW, NOW)
        );
    }

    @Test
    void shouldRejectVoteWhenSessionIsClosed() {
        VotingSession session = VotingSession.open(
            VotingSessionId.newId(),
            AgendaId.newId(),
            VotingSessionDuration.from(Duration.ofSeconds(10)),
            NOW
        );

        assertThrows(VotingSessionClosedException.class, () -> session.ensureOpenAt(NOW.plusSeconds(10)));
        assertTrue(session.isOpenAt(NOW.plusSeconds(9)));
    }
}
