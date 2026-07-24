package br.com.cooperativevoting.voting.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registro de voto.")
public record RegisterVoteRequest(
    @NotBlank(message = "O identificador da pauta é obrigatório.")
    @Schema(example = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2", requiredMode = Schema.RequiredMode.REQUIRED)
    String pautaId,

    @NotBlank(message = "O identificador do associado é obrigatório.")
    @Size(max = 64, message = "O identificador do associado deve ter no máximo 64 caracteres.")
    @Schema(example = "associado-123", requiredMode = Schema.RequiredMode.REQUIRED)
    String associadoId,

    @NotBlank(message = "O CPF do associado é obrigatório.")
    @Pattern(
        regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$",
        message = "O CPF deve conter 11 dígitos, com ou sem pontuação."
    )
    @Schema(example = "529.982.247-25", requiredMode = Schema.RequiredMode.REQUIRED)
    String cpf,

    @NotBlank(message = "O voto é obrigatório.")
    @Pattern(
        regexp = "(?i)^(sim|s|yes|y|nao|não|n|no)$",
        message = "O voto deve ser SIM ou NAO."
    )
    @Schema(example = "SIM", allowableValues = {"SIM", "NAO"}, requiredMode = Schema.RequiredMode.REQUIRED)
    String voto
) {
}
