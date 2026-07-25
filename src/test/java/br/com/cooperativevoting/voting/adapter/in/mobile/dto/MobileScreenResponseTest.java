package br.com.cooperativevoting.voting.adapter.in.mobile.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MobileScreenResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAllSupportedFormFieldTypesAccordingToMobileContract() throws Exception {
        MobileScreenResponse screen = new MobileScreenResponse(
            MobileScreenType.FORMULARIO,
            "Tela contrato",
            List.of(
                MobileScreenItemResponse.text("Texto informativo"),
                MobileScreenItemResponse.inputText("idCampoTexto", "Campo de texto", "Texto"),
                MobileScreenItemResponse.inputNumber("idCampoNumerico", "Campo numerico", 999),
                MobileScreenItemResponse.inputDate("idCampoData", "Campo data", "01/01/2000")
            ),
            new MobileScreenButtonResponse("Acao 1", "/ACAO1", Map.of("campo1", "valor1", "campo2", 123)),
            new MobileScreenButtonResponse("Cancelar", "/", Map.of())
        );

        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(screen));

        assertThat(root.path("tipo").asText()).isEqualTo("FORMULARIO");
        assertThat(root.path("titulo").asText()).isEqualTo("Tela contrato");
        assertThat(root.path("itens").size()).isEqualTo(4);
        assertThat(root.at("/itens/0/tipo").asText()).isEqualTo("TEXTO");
        assertThat(root.at("/itens/1/tipo").asText()).isEqualTo("INPUT_TEXTO");
        assertThat(root.at("/itens/2/tipo").asText()).isEqualTo("INPUT_NUMERO");
        assertThat(root.at("/itens/3/tipo").asText()).isEqualTo("INPUT_DATA");
        assertThat(root.at("/itens/3/id").asText()).isEqualTo("idCampoData");
        assertThat(root.at("/itens/3/valor").asText()).isEqualTo("01/01/2000");
        assertThat(root.at("/botaoOk/texto").asText()).isEqualTo("Acao 1");
        assertThat(root.at("/botaoOk/url").asText()).isEqualTo("/ACAO1");
        assertThat(root.at("/botaoOk/body/campo1").asText()).isEqualTo("valor1");
        assertThat(root.at("/botaoOk/body/campo2").asInt()).isEqualTo(123);
        assertThat(root.toString()).doesNotContain("null");
    }

    @Test
    void shouldSerializeSelectionItemsWithCallbackBodyAccordingToMobileContract() throws Exception {
        MobileScreenResponse screen = new MobileScreenResponse(
            MobileScreenType.SELECAO,
            "Lista de selecao",
            List.of(MobileScreenItemResponse.selection(
                "Opcao 1",
                "/OPT1",
                Map.of("dadosOpcao", "campo de teste")
            )),
            null,
            null
        );

        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(screen));

        assertThat(root.path("tipo").asText()).isEqualTo("SELECAO");
        assertThat(root.path("titulo").asText()).isEqualTo("Lista de selecao");
        assertThat(root.path("botaoOk").isMissingNode()).isTrue();
        assertThat(root.at("/itens/0/texto").asText()).isEqualTo("Opcao 1");
        assertThat(root.at("/itens/0/url").asText()).isEqualTo("/OPT1");
        assertThat(root.at("/itens/0/body/dadosOpcao").asText()).isEqualTo("campo de teste");
        assertThat(root.at("/itens/0/tipo").isMissingNode()).isTrue();
        assertThat(root.toString()).doesNotContain("null");
    }
}
