package br.com.cooperativevoting.voting.application.usecase.registervote.dto;

public record RegisterVoteInput(
    String agendaId,
    String associateId,
    String cpf,
    String choice
) {
}
