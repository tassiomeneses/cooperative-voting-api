package br.com.cooperativevoting.voting.adapter.in.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Botão de ação interpretado pelo cliente mobile.")
public record MobileScreenButtonResponse(
    @Schema(example = "Confirmar")
    String texto,

    @Schema(example = "/v1/mobile/pautas")
    String url,

    @Schema(description = "Corpo base enviado pelo app ao acionar o botão.")
    Map<String, Object> body
) {
}
