package br.com.cooperativevoting.voting.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pauta cadastrada.")
public record CreateAgendaResponse(
    @Schema(example = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2")
    String id,

    @Schema(example = "Aprovação do relatório anual")
    String titulo,

    @Schema(example = "Votação para aprovação do relatório anual da cooperativa.")
    String descricao,

    @Schema(example = "2026-07-24T12:00:00Z")
    Instant criadaEm
) {
}
