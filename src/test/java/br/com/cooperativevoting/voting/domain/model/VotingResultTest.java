package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.factory.VotingResultFactory;
import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VotingResultTest {

    @Test
    void shouldCalculateApprovedOutcome() {
        VotingResult result = VotingResultFactory.fromTally(AgendaId.newId(), 10, 4);

        assertEquals(14, result.totalVotes());
        assertEquals(VotingResultOutcome.APPROVED, result.outcome());
    }

    @Test
    void shouldCalculateRejectedOutcome() {
        VotingResult result = VotingResultFactory.fromTally(AgendaId.newId(), 3, 8);

        assertEquals(VotingResultOutcome.REJECTED, result.outcome());
    }

    @Test
    void shouldCalculateTiedOutcome() {
        VotingResult result = VotingResultFactory.fromTally(AgendaId.newId(), 5, 5);

        assertEquals(VotingResultOutcome.TIED, result.outcome());
    }

    @Test
    void shouldRejectNegativeVoteCount() {
        assertThrows(
            InvalidDomainObjectException.class,
            () -> VotingResultFactory.fromTally(AgendaId.newId(), -1, 0)
        );
    }
}
