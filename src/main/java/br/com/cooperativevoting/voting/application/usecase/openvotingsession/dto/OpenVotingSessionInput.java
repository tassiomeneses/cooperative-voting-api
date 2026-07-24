package br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto;

public record OpenVotingSessionInput(
    String agendaId,
    Long durationMinutes
) {
}
