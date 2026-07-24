package br.com.cooperativevoting.voting.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da votação de uma pauta.")
public record VotingResultResponse(
    @Schema(example = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2")
    String pautaId,

    @Schema(example = "10")
    long votosSim,

    @Schema(example = "4")
    long votosNao,

    @Schema(example = "14")
    long totalVotos,

    @Schema(example = "APROVADA")
    String resultado
) {
}
