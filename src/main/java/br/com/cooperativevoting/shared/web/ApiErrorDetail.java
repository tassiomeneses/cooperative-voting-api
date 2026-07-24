package br.com.cooperativevoting.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalhe de validação ou erro específico.")
public record ApiErrorDetail(
    @Schema(example = "titulo")
    String field,

    @Schema(example = "O título da pauta é obrigatório.")
    String message
) {
}
