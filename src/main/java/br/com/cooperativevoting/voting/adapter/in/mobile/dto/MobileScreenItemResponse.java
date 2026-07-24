package br.com.cooperativevoting.voting.adapter.in.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Item de tela interpretado pelo cliente mobile.")
public record MobileScreenItemResponse(
    @Schema(example = "INPUT_TEXTO")
    MobileScreenItemType tipo,

    @Schema(example = "titulo")
    String id,

    @Schema(example = "Titulo da pauta")
    String titulo,

    @Schema(example = "Aprovacao do relatorio anual")
    Object valor,

    @Schema(example = "Opcao 1")
    String texto,

    @Schema(example = "/v1/mobile/votos")
    String url,

    @Schema(description = "Corpo base enviado pelo app ao acionar um item de selecao.")
    Map<String, Object> body
) {
    public static MobileScreenItemResponse text(String text) {
        return new MobileScreenItemResponse(MobileScreenItemType.TEXTO, null, null, null, text, null, null);
    }

    public static MobileScreenItemResponse inputText(String id, String title, Object value) {
        return new MobileScreenItemResponse(MobileScreenItemType.INPUT_TEXTO, id, title, value, null, null, null);
    }

    public static MobileScreenItemResponse inputNumber(String id, String title, Number value) {
        return new MobileScreenItemResponse(MobileScreenItemType.INPUT_NUMERO, id, title, value, null, null, null);
    }

    public static MobileScreenItemResponse selection(String text, String url, Map<String, Object> body) {
        return new MobileScreenItemResponse(null, null, null, null, text, url, body);
    }
}
