package br.com.cooperativevoting.voting.adapter.in.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados coletados antes da tela de selecao de voto.")
public record MobileVoteIdentificationRequest(
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
    String cpf
) {
}
