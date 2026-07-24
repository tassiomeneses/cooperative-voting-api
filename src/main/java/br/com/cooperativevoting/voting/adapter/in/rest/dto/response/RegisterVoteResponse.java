package br.com.cooperativevoting.voting.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Voto registrado.")
public record RegisterVoteResponse(
    @Schema(example = "61791fb7-d241-4d43-a835-29a9c741c7e2")
    String id,

    @Schema(example = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2")
    String pautaId,

    @Schema(example = "associado-123")
    String associadoId,

    @Schema(example = "SIM")
    String voto,

    @Schema(example = "2026-07-24T12:00:00Z")
    Instant votadoEm
) {
}
