package br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto;

import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;

public record FindVotingResultOutput(
    String agendaId,
    long yesVotes,
    long noVotes,
    long totalVotes,
    VotingResultOutcome outcome
) {
}
