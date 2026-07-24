package br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto;

import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;

import java.time.Instant;

public record OpenVotingSessionOutput(
    String agendaId,
    String votingSessionId,
    Instant openedAt,
    Instant closedAt,
    VotingSessionStatus status
) {
}
