package br.com.cooperativevoting.voting.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de uma nova pauta.")
public record CreateAgendaRequest(
    @NotBlank(message = "O título da pauta é obrigatório.")
    @Size(max = 120, message = "O título da pauta deve ter no máximo 120 caracteres.")
    @Schema(example = "Aprovação do relatório anual", requiredMode = Schema.RequiredMode.REQUIRED)
    String titulo,

    @Size(max = 500, message = "A descrição da pauta deve ter no máximo 500 caracteres.")
    @Schema(example = "Votação para aprovação do relatório anual da cooperativa.")
    String descricao
) {
}
