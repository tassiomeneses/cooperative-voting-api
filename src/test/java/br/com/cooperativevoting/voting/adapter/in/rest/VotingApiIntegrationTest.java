package br.com.cooperativevoting.voting.adapter.in.rest;

import br.com.cooperativevoting.testsupport.PostgreSqlContainerSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static br.com.cooperativevoting.voting.adapter.out.client.userinfo.UserInfoClientConfiguration.ASSOCIATE_ELIGIBILITY_CACHE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VotingApiIntegrationTest extends PostgreSqlContainerSupport {

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
    static void registerUserInfoProperties(DynamicPropertyRegistry registry) {
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
    void shouldExecuteCompleteVotingFlow() throws Exception {
        stubAbleToVote();

        String agendaId = createAgenda();
        openVotingSession(agendaId, 5);

        registerVote(agendaId, "associate-1", VALID_CPF, "SIM")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.voto").value("SIM"));

        registerVote(agendaId, "associate-2", SECOND_VALID_CPF, "NAO")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.voto").value("NAO"));

        mockMvc.perform(get("/pautas/{id}/resultado", agendaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.votosSim").value(1))
            .andExpect(jsonPath("$.data.votosNao").value(1))
            .andExpect(jsonPath("$.data.totalVotos").value(2))
            .andExpect(jsonPath("$.data.resultado").value("EMPATADA"));
    }

    @Test
    void shouldReturnConflictWhenCpfVotesTwiceWithDifferentAssociateIds() throws Exception {
        stubAbleToVote();

        String agendaId = createAgenda();
        openVotingSession(agendaId, 5);

        registerVote(agendaId, "associate-1", VALID_CPF, "SIM")
            .andExpect(status().isCreated());

        registerVote(agendaId, "associate-2", VALID_CPF, "NAO")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Este associado já votou nesta pauta."))
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_VOTE"));
    }

    @Test
    void shouldReturnForbiddenWhenAssociateIsUnableToVote() throws Exception {
        wireMock.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(okJson("""
                {"status":"UNABLE_TO_VOTE"}
                """)));

        String agendaId = createAgenda();
        openVotingSession(agendaId, 5);

        registerVote(agendaId, "associate-1", VALID_CPF, "SIM")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Este associado não está apto a votar."))
            .andExpect(jsonPath("$.error.code").value("ASSOCIATE_UNABLE_TO_VOTE"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenEligibilityServiceFails() throws Exception {
        wireMock.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse().withStatus(503)));

        String agendaId = createAgenda();
        openVotingSession(agendaId, 5);

        registerVote(agendaId, "associate-1", VALID_CPF, "SIM")
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("Não foi possível consultar se o associado está apto a votar. Tente novamente em instantes."))
            .andExpect(jsonPath("$.error.code").value("ASSOCIATE_ELIGIBILITY_UNAVAILABLE"));
    }

    @Test
    void shouldReturnBadRequestForInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":" "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private String createAgenda() throws Exception {
        MvcResult result = mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Aprovação do relatório anual",
                      "descricao": "Relatório anual da cooperativa."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.at("/data/id").asText();
    }

    private void openVotingSession(String agendaId, long durationMinutes) throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"duracaoMinutos": %d}
                    """.formatted(durationMinutes)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions registerVote(
        String agendaId,
        String associateId,
        String cpf,
        String vote
    ) throws Exception {
        return mockMvc.perform(post("/votos")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "pautaId": "%s",
                  "associadoId": "%s",
                  "cpf": "%s",
                  "voto": "%s"
                }
                """.formatted(agendaId, associateId, cpf, vote)));
    }

    private void stubAbleToVote() {
        wireMock.stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(okJson("""
                {"status":"ABLE_TO_VOTE"}
                """)));
    }
}
