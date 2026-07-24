package br.com.cooperativevoting.voting.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Sessão de votação aberta.")
public record OpenVotingSessionResponse(
    @Schema(example = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2")
    String pautaId,

    @Schema(example = "f61a36bc-4ca2-4731-a308-44c615bd8331")
    String sessaoId,

    @Schema(example = "2026-07-24T12:00:00Z")
    Instant abertaEm,

    @Schema(example = "2026-07-24T12:01:00Z")
    Instant fechaEm,

    @Schema(example = "ABERTA")
    String status
) {
}
