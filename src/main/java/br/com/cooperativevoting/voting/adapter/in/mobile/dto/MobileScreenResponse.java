package br.com.cooperativevoting.voting.adapter.in.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Tela dinamica interpretada pelo cliente mobile conforme o Anexo 1.")
public record MobileScreenResponse(
    @Schema(example = "FORMULARIO")
    MobileScreenType tipo,

    @Schema(example = "Cadastrar pauta")
    String titulo,

    List<MobileScreenItemResponse> itens,

    MobileScreenButtonResponse botaoOk,

    MobileScreenButtonResponse botaoCancelar
) {
}
