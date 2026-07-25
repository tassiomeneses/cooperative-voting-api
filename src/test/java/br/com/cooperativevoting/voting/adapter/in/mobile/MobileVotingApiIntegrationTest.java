package br.com.cooperativevoting.voting.adapter.in.mobile;

import br.com.cooperativevoting.testsupport.PostgreSqlContainerSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.cooperativevoting.voting.adapter.out.client.userinfo.UserInfoClientConfiguration.ASSOCIATE_ELIGIBILITY_CACHE;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MobileVotingApiIntegrationTest extends PostgreSqlContainerSupport {

    private static final Pattern AGENDA_ID_PATTERN = Pattern.compile("/v1/mobile/pautas/([^/]+)/");
    private static final String VALID_CPF = "529.982.247-25";
    private static final String SECOND_VALID_CPF = "111.444.777-35";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("integrations.user-info.base-url", wireMock::baseUrl);
        registry.add("integrations.user-info.retry.period", () -> "10ms");
        registry.add("integrations.user-info.retry.max-period", () -> "10ms");
        registry.add("integrations.user-info.retry.max-attempts", () -> "2");
        registry.add("integrations.user-info.cache.ttl", () -> "10m");
        registry.add("security.identity-hash.pepper", () -> "test-pepper");
        registry.add("spring.cloud.openfeign.client.config.user-info.connectTimeout", () -> "1000");
        registry.add("spring.cloud.openfeign.client.config.user-info.readTimeout", () -> "5000");
        registry.add("resilience4j.circuitbreaker.instances.userInfo.minimumNumberOfCalls", () -> "100");
        registry.add("resilience4j.timelimiter.instances.userInfo.timeoutDuration", () -> "6s");
    }

    @BeforeEach
    void setUp() {
        configureFor("localhost", wireMock.getPort());
        wireMock.resetAll();
        jdbcTemplate.execute("TRUNCATE TABLE votes, voting_sessions, agendas CASCADE");
        Cache cache = cacheManager.getCache(ASSOCIATE_ELIGIBILITY_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldExecuteCompleteVotingFlowUsingOnlyMobileContract() throws Exception {
        stubAbleToVote();

        mockMvc.perform(post("/v1/mobile/pautas/nova"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.itens[0].tipo").value("INPUT_TEXTO"))
            .andExpect(jsonPath("$.botaoOk.url").value("/v1/mobile/pautas"));

        String agendaId = createAgendaUsingMobileContract();
        openVotingSessionUsingMobileContract(agendaId);

        mockMvc.perform(post("/v1/mobile/pautas/{id}/voto/identificacao", agendaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.botaoOk.url").value("/v1/mobile/pautas/" + agendaId + "/voto/opcoes"));

        JsonNode firstVoteBody = findVoteBodyFromSelectionScreen(agendaId, "associate-1", VALID_CPF, "SIM");
        registerVoteUsingMobileContract(firstVoteBody)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Voto registrado"));

        JsonNode secondVoteBody = findVoteBodyFromSelectionScreen(agendaId, "associate-2", SECOND_VALID_CPF, "NAO");
        registerVoteUsingMobileContract(secondVoteBody)
            .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/mobile/pautas/{id}/resultado", agendaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Resultado da votacao"))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Resultado: EMPATADA")))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Sim: 1")))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Nao: 1")))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Total: 2")));
    }

    @Test
    void shouldReturnMobileContractSelectionWithCallbackBody() throws Exception {
        String agendaId = createAgendaUsingMobileContract();

        mockMvc.perform(post("/v1/mobile/pautas/{id}/voto/opcoes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "associadoId": "associate-1",
                      "cpf": "%s"
                    }
                    """.formatted(VALID_CPF)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("SELECAO"))
            .andExpect(jsonPath("$.itens[0].texto").value("Sim"))
            .andExpect(jsonPath("$.itens[0].url").value("/v1/mobile/votos"))
            .andExpect(jsonPath("$.itens[0].body.pautaId").value(agendaId))
            .andExpect(jsonPath("$.itens[0].body.associadoId").value("associate-1"))
            .andExpect(jsonPath("$.itens[0].body.cpf").value(VALID_CPF))
            .andExpect(jsonPath("$.itens[0].body.voto").value("SIM"))
            .andExpect(jsonPath("$.itens[1].texto").value("Nao"))
            .andExpect(jsonPath("$.itens[1].body.voto").value("NAO"));
    }

    @Test
    void shouldReturnForbiddenFromMobileVoteWhenAssociateIsUnableToVote() throws Exception {
        wireMock.stubFor(WireMock.get(urlPathMatching("/users/.*"))
            .willReturn(okJson("""
                {"status":"UNABLE_TO_VOTE"}
                """)));

        String agendaId = createAgendaUsingMobileContract();
        openVotingSessionUsingMobileContract(agendaId);
        JsonNode voteBody = findVoteBodyFromSelectionScreen(agendaId, "associate-1", VALID_CPF, "SIM");

        registerVoteUsingMobileContract(voteBody)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Este associado não está apto a votar."))
            .andExpect(jsonPath("$.error.code").value("ASSOCIATE_UNABLE_TO_VOTE"));
    }

    @Test
    void shouldReturnConflictFromMobileVoteWhenAssociateVotesTwice() throws Exception {
        stubAbleToVote();

        String agendaId = createAgendaUsingMobileContract();
        openVotingSessionUsingMobileContract(agendaId);
        JsonNode firstVoteBody = findVoteBodyFromSelectionScreen(agendaId, "associate-1", VALID_CPF, "SIM");
        JsonNode duplicatedVoteBody = findVoteBodyFromSelectionScreen(agendaId, "associate-1", SECOND_VALID_CPF, "NAO");

        registerVoteUsingMobileContract(firstVoteBody)
            .andExpect(status().isCreated());

        registerVoteUsingMobileContract(duplicatedVoteBody)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_VOTE"));
    }

    private String createAgendaUsingMobileContract() throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/mobile/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Aprovacao do relatorio anual",
                      "descricao": "Relatorio anual da cooperativa."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.botaoOk.texto").value("Abrir sessao"))
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return extractAgendaId(root.at("/botaoOk/url").asText());
    }

    private void openVotingSessionUsingMobileContract(String agendaId) throws Exception {
        mockMvc.perform(post("/v1/mobile/pautas/{id}/sessao", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"duracaoMinutos": 5}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.botaoOk.url").value("/v1/mobile/pautas/" + agendaId + "/voto/identificacao"));
    }

    private JsonNode findVoteBodyFromSelectionScreen(
        String agendaId,
        String associateId,
        String cpf,
        String vote
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/mobile/pautas/{id}/voto/opcoes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "associadoId": "%s",
                      "cpf": "%s"
                    }
                    """.formatted(associateId, cpf)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("SELECAO"))
            .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("itens");
        for (JsonNode item : items) {
            if (vote.equals(item.at("/body/voto").asText())) {
                return item.path("body");
            }
        }
        throw new AssertionError("Vote option not found: " + vote);
    }

    private org.springframework.test.web.servlet.ResultActions registerVoteUsingMobileContract(JsonNode voteBody) throws Exception {
        return mockMvc.perform(post("/v1/mobile/votos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(voteBody)));
    }

    private String extractAgendaId(String url) {
        Matcher matcher = AGENDA_ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new AssertionError("Agenda id not found in mobile callback URL: " + url);
        }
        return matcher.group(1);
    }

    private void stubAbleToVote() {
        wireMock.stubFor(WireMock.get(urlPathMatching("/users/.*"))
            .willReturn(okJson("""
                {"status":"ABLE_TO_VOTE"}
                """)));
    }
}
