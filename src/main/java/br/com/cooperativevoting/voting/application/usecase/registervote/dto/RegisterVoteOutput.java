package br.com.cooperativevoting.voting.application.usecase.registervote.dto;

import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;

import java.time.Instant;

public record RegisterVoteOutput(
    String voteId,
    String agendaId,
    String associateId,
    VoteChoice choice,
    Instant castAt
) {
}
