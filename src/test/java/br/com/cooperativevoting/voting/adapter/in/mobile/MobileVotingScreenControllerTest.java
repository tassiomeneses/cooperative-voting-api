package br.com.cooperativevoting.voting.adapter.in.mobile;

import br.com.cooperativevoting.shared.error.GlobalExceptionHandler;
import br.com.cooperativevoting.voting.adapter.in.mobile.dto.MobileVoteIdentificationRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.CreateAgendaRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.OpenVotingSessionRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.RegisterVoteRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.AgendaRestMapper;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.VoteRestMapper;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.VotingResultRestMapper;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.VotingSessionRestMapper;
import br.com.cooperativevoting.voting.application.port.in.CreateAgendaUseCase;
import br.com.cooperativevoting.voting.application.port.in.FindVotingResultUseCase;
import br.com.cooperativevoting.voting.application.port.in.OpenVotingSessionUseCase;
import br.com.cooperativevoting.voting.application.port.in.RegisterVoteUseCase;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaInput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;
import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MobileVotingScreenControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final String AGENDA_ID = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2";
    private static final String SESSION_ID = "f61a36bc-4ca2-4731-a308-44c615bd8331";
    private static final String VOTE_ID = "61791fb7-d241-4d43-a835-29a9c741c7e2";

    @Mock
    private CreateAgendaUseCase createAgendaUseCase;

    @Mock
    private OpenVotingSessionUseCase openVotingSessionUseCase;

    @Mock
    private RegisterVoteUseCase registerVoteUseCase;

    @Mock
    private FindVotingResultUseCase findVotingResultUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        MobileVotingScreenController controller = new MobileVotingScreenController(
            createAgendaUseCase,
            openVotingSessionUseCase,
            registerVoteUseCase,
            findVotingResultUseCase,
            new AgendaRestMapper(),
            new VotingSessionRestMapper(),
            new VoteRestMapper(),
            new VotingResultRestMapper(),
            new MobileCallbackUrlBuilder("https://api.example.com/")
        );

        objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @Test
    void shouldReturnCreateAgendaFormScreen() throws Exception {
        mockMvc.perform(post("/v1/mobile/pautas/nova"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Cadastrar pauta"))
            .andExpect(jsonPath("$.itens[0].tipo").value("INPUT_TEXTO"))
            .andExpect(jsonPath("$.itens[0].id").value("titulo"))
            .andExpect(jsonPath("$.itens[1].id").value("descricao"))
            .andExpect(jsonPath("$.botaoOk.texto").value("Cadastrar"))
            .andExpect(jsonPath("$.botaoOk.url").value("https://api.example.com/v1/mobile/pautas"))
            .andExpect(jsonPath("$.botaoOk.body").isMap());
    }

    @Test
    void shouldCreateAgendaAndReturnNextMobileScreen() throws Exception {
        CreateAgendaOutput output = new CreateAgendaOutput(
            AGENDA_ID,
            "Aprovacao do relatorio anual",
            "Relatorio anual da cooperativa.",
            NOW
        );
        when(createAgendaUseCase.execute(new CreateAgendaInput(
            "Aprovacao do relatorio anual",
            "Relatorio anual da cooperativa."
        ))).thenReturn(output);

        mockMvc.perform(post("/v1/mobile/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAgendaRequest(
                    "Aprovacao do relatorio anual",
                    "Relatorio anual da cooperativa."
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Pauta cadastrada"))
            .andExpect(jsonPath("$.itens[0].tipo").value("TEXTO"))
            .andExpect(jsonPath("$.itens[0].texto", containsString(AGENDA_ID)))
            .andExpect(jsonPath("$.botaoOk.texto").value("Abrir sessao"))
            .andExpect(jsonPath("$.botaoOk.url").value("https://api.example.com/v1/mobile/pautas/" + AGENDA_ID + "/sessao/nova"));

        verify(createAgendaUseCase).execute(new CreateAgendaInput(
            "Aprovacao do relatorio anual",
            "Relatorio anual da cooperativa."
        ));
    }

    @Test
    void shouldReturnOpenSessionFormScreen() throws Exception {
        mockMvc.perform(post("/v1/mobile/pautas/{id}/sessao/nova", AGENDA_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Abrir sessao de votacao"))
            .andExpect(jsonPath("$.itens[1].tipo").value("INPUT_NUMERO"))
            .andExpect(jsonPath("$.itens[1].id").value("duracaoMinutos"))
            .andExpect(jsonPath("$.itens[1].valor").value(1))
            .andExpect(jsonPath("$.botaoOk.url").value("https://api.example.com/v1/mobile/pautas/" + AGENDA_ID + "/sessao"));
    }

    @Test
    void shouldOpenVotingSessionAndReturnConfirmationScreen() throws Exception {
        OpenVotingSessionOutput output = new OpenVotingSessionOutput(
            AGENDA_ID,
            SESSION_ID,
            NOW,
            NOW.plusSeconds(300),
            VotingSessionStatus.OPEN
        );
        when(openVotingSessionUseCase.execute(new OpenVotingSessionInput(AGENDA_ID, 5L))).thenReturn(output);

        mockMvc.perform(post("/v1/mobile/pautas/{id}/sessao", AGENDA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenVotingSessionRequest(5L))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Sessao aberta"))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Duracao: 5 minuto(s).")))
            .andExpect(jsonPath("$.botaoOk.texto").value("Votar"))
            .andExpect(jsonPath("$.botaoOk.url").value("https://api.example.com/v1/mobile/pautas/" + AGENDA_ID + "/voto/identificacao"));
    }

    @Test
    void shouldReturnVoteIdentificationFormScreen() throws Exception {
        mockMvc.perform(post("/v1/mobile/pautas/{id}/voto/identificacao", AGENDA_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Identificar associado"))
            .andExpect(jsonPath("$.itens[0].id").value("associadoId"))
            .andExpect(jsonPath("$.itens[1].id").value("cpf"))
            .andExpect(jsonPath("$.botaoOk.body.pautaId").value(AGENDA_ID));
    }

    @Test
    void shouldReturnVoteSelectionScreen() throws Exception {
        mockMvc.perform(post("/v1/mobile/pautas/{id}/voto/opcoes", AGENDA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MobileVoteIdentificationRequest(
                    "associado-123",
                    "529.982.247-25"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("SELECAO"))
            .andExpect(jsonPath("$.titulo").value("Escolha seu voto"))
            .andExpect(jsonPath("$.itens[0].texto").value("Sim"))
            .andExpect(jsonPath("$.itens[0].url").value("https://api.example.com/v1/mobile/votos"))
            .andExpect(jsonPath("$.itens[0].body.pautaId").value(AGENDA_ID))
            .andExpect(jsonPath("$.itens[0].body.associadoId").value("associado-123"))
            .andExpect(jsonPath("$.itens[0].body.cpf").value("529.982.247-25"))
            .andExpect(jsonPath("$.itens[0].body.voto").value("SIM"))
            .andExpect(jsonPath("$.itens[1].texto").value("Nao"))
            .andExpect(jsonPath("$.itens[1].body.voto").value("NAO"));
    }

    @Test
    void shouldRegisterVoteAndReturnConfirmationScreen() throws Exception {
        RegisterVoteOutput output = new RegisterVoteOutput(
            VOTE_ID,
            AGENDA_ID,
            "associado-123",
            VoteChoice.YES,
            NOW
        );
        RegisterVoteInput input = new RegisterVoteInput(AGENDA_ID, "associado-123", "529.982.247-25", "SIM");
        when(registerVoteUseCase.execute(input)).thenReturn(output);

        mockMvc.perform(post("/v1/mobile/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "SIM"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Voto registrado"))
            .andExpect(jsonPath("$.itens[0].texto", containsString(VOTE_ID)))
            .andExpect(jsonPath("$.botaoOk.texto").value("Resultado"))
            .andExpect(jsonPath("$.botaoOk.url").value("https://api.example.com/v1/mobile/pautas/" + AGENDA_ID + "/resultado"));
    }

    @Test
    void shouldReturnVotingResultScreen() throws Exception {
        FindVotingResultOutput output = new FindVotingResultOutput(
            AGENDA_ID,
            10,
            4,
            14,
            VotingResultOutcome.APPROVED
        );
        when(findVotingResultUseCase.execute(new FindVotingResultInput(AGENDA_ID))).thenReturn(output);

        mockMvc.perform(post("/v1/mobile/pautas/{id}/resultado", AGENDA_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
            .andExpect(jsonPath("$.titulo").value("Resultado da votacao"))
            .andExpect(jsonPath("$.itens[0].tipo").value("TEXTO"))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Resultado: APROVADA")))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Sim: 10")))
            .andExpect(jsonPath("$.itens[0].texto", containsString("Nao: 4")))
            .andExpect(jsonPath("$.botaoOk.texto").value("Votar"));
    }

    @Test
    void shouldRejectInvalidVoteIdentification() throws Exception {
        mockMvc.perform(post("/v1/mobile/pautas/{id}/voto/opcoes", AGENDA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MobileVoteIdentificationRequest("", "123"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
