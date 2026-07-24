package br.com.cooperativevoting.voting.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(description = "Dados para abertura de sessão de votação.")
public record OpenVotingSessionRequest(
    @Positive(message = "A duração da sessão deve ser maior que zero.")
    @Max(value = 1440, message = "A duração da sessão deve ter no máximo 1440 minutos.")
    @Schema(description = "Duração da sessão em minutos. Quando ausente, a API usa 1 minuto.", example = "5")
    Long duracaoMinutos
) {
}
