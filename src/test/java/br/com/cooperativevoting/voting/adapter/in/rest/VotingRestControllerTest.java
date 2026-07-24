package br.com.cooperativevoting.voting.adapter.in.rest;

import br.com.cooperativevoting.shared.error.GlobalExceptionHandler;
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
import br.com.cooperativevoting.voting.application.exception.AssociateEligibilityUnavailableException;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.exception.AssociateUnableToVoteException;
import br.com.cooperativevoting.voting.domain.exception.DuplicateVoteException;
import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;
import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VotingRestControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final String AGENDA_ID = "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2";
    private static final String SESSION_ID = "f61a36bc-4ca2-4731-a308-44c615bd8331";
    private static final String VOTE_ID = "61791fb7-d241-4d43-a835-29a9c741c7e2";

    @Mock
    private CreateAgendaUseCase createAgendaUseCase;

    @Mock
    private OpenVotingSessionUseCase openVotingSessionUseCase;

    @Mock
    private FindVotingResultUseCase findVotingResultUseCase;

    @Mock
    private RegisterVoteUseCase registerVoteUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AgendaRestController agendaRestController = new AgendaRestController(
            createAgendaUseCase,
            openVotingSessionUseCase,
            findVotingResultUseCase,
            new AgendaRestMapper(),
            new VotingSessionRestMapper(),
            new VotingResultRestMapper()
        );
        VoteRestController voteRestController = new VoteRestController(registerVoteUseCase, new VoteRestMapper());
        objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
            .standaloneSetup(agendaRestController, voteRestController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @Test
    void shouldCreateAgenda() throws Exception {
        CreateAgendaOutput output = new CreateAgendaOutput(
            AGENDA_ID,
            "Aprovação do relatório anual",
            "Relatório anual da cooperativa.",
            NOW
        );
        when(createAgendaUseCase.execute(new CreateAgendaInput(
            "Aprovação do relatório anual",
            "Relatório anual da cooperativa."
        ))).thenReturn(output);

        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAgendaRequest(
                    "Aprovação do relatório anual",
                    "Relatório anual da cooperativa."
                ))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/pautas/" + AGENDA_ID)))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Pauta cadastrada com sucesso."))
            .andExpect(jsonPath("$.data.id").value(AGENDA_ID))
            .andExpect(jsonPath("$.data.titulo").value("Aprovação do relatório anual"));
    }

    @Test
    void shouldExposeVersionedCreateAgendaEndpoint() throws Exception {
        CreateAgendaOutput output = new CreateAgendaOutput(
            AGENDA_ID,
            "Aprovação do relatório anual",
            "Relatório anual da cooperativa.",
            NOW
        );
        when(createAgendaUseCase.execute(new CreateAgendaInput(
            "Aprovação do relatório anual",
            "Relatório anual da cooperativa."
        ))).thenReturn(output);

        mockMvc.perform(post("/v1/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAgendaRequest(
                    "Aprovação do relatório anual",
                    "Relatório anual da cooperativa."
                ))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/v1/pautas/" + AGENDA_ID)))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRejectCreateAgendaWithInvalidPayload() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAgendaRequest(" ", null))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Existem campos inválidos na requisição."))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details[0].field").value("titulo"));
    }

    @Test
    void shouldOpenVotingSessionWithDefaultDuration() throws Exception {
        OpenVotingSessionOutput output = new OpenVotingSessionOutput(
            AGENDA_ID,
            SESSION_ID,
            NOW,
            NOW.plusSeconds(60),
            VotingSessionStatus.OPEN
        );
        when(openVotingSessionUseCase.execute(new OpenVotingSessionInput(AGENDA_ID, null))).thenReturn(output);

        mockMvc.perform(post("/pautas/{id}/sessao", AGENDA_ID))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Sessão de votação aberta com sucesso."))
            .andExpect(jsonPath("$.data.pautaId").value(AGENDA_ID))
            .andExpect(jsonPath("$.data.sessaoId").value(SESSION_ID))
            .andExpect(jsonPath("$.data.status").value("ABERTA"));
    }

    @Test
    void shouldOpenVotingSessionWithCustomDuration() throws Exception {
        OpenVotingSessionOutput output = new OpenVotingSessionOutput(
            AGENDA_ID,
            SESSION_ID,
            NOW,
            NOW.plusSeconds(300),
            VotingSessionStatus.OPEN
        );
        when(openVotingSessionUseCase.execute(new OpenVotingSessionInput(AGENDA_ID, 5L))).thenReturn(output);

        mockMvc.perform(post("/pautas/{id}/sessao", AGENDA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenVotingSessionRequest(5L))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.fechaEm").value("2026-07-24T12:05:00Z"));
    }

    @Test
    void shouldRejectOpenVotingSessionWhenDurationExceedsMaximum() throws Exception {
        mockMvc.perform(post("/pautas/{id}/sessao", AGENDA_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenVotingSessionRequest(1441L))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details[0].field").value("duracaoMinutos"));
    }

    @Test
    void shouldRegisterVote() throws Exception {
        RegisterVoteOutput output = new RegisterVoteOutput(
            VOTE_ID,
            AGENDA_ID,
            "associado-123",
            VoteChoice.YES,
            NOW
        );
        RegisterVoteInput expectedInput = new RegisterVoteInput(
            AGENDA_ID,
            "associado-123",
            "529.982.247-25",
            "SIM"
        );
        when(registerVoteUseCase.execute(expectedInput)).thenReturn(output);

        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "SIM"
                ))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/votos/" + VOTE_ID)))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Voto registrado com sucesso."))
            .andExpect(jsonPath("$.data.id").value(VOTE_ID))
            .andExpect(jsonPath("$.data.voto").value("SIM"));
    }

    @Test
    void shouldRejectVoteWithInvalidPayload() throws Exception {
        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "",
                    "123",
                    "TALVEZ"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldFindVotingResult() throws Exception {
        FindVotingResultOutput output = new FindVotingResultOutput(
            AGENDA_ID,
            10,
            4,
            14,
            VotingResultOutcome.APPROVED
        );
        when(findVotingResultUseCase.execute(new FindVotingResultInput(AGENDA_ID))).thenReturn(output);

        mockMvc.perform(get("/pautas/{id}/resultado", AGENDA_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Resultado da votação encontrado com sucesso."))
            .andExpect(jsonPath("$.data.pautaId").value(AGENDA_ID))
            .andExpect(jsonPath("$.data.votosSim").value(10))
            .andExpect(jsonPath("$.data.votosNao").value(4))
            .andExpect(jsonPath("$.data.totalVotos").value(14))
            .andExpect(jsonPath("$.data.resultado").value("APROVADA"));
    }

    @Test
    void shouldReturnNotFoundWhenAgendaDoesNotExist() throws Exception {
        AgendaId agendaId = AgendaId.from(UUID.fromString(AGENDA_ID));
        when(findVotingResultUseCase.execute(new FindVotingResultInput(AGENDA_ID)))
            .thenThrow(new AgendaNotFoundException(agendaId));

        mockMvc.perform(get("/pautas/{id}/resultado", AGENDA_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Pauta não encontrada."))
            .andExpect(jsonPath("$.error.code").value("AGENDA_NOT_FOUND"));
    }

    @Test
    void shouldReturnConflictWhenAgendaAlreadyHasSession() throws Exception {
        AgendaId agendaId = AgendaId.from(UUID.fromString(AGENDA_ID));
        when(openVotingSessionUseCase.execute(any(OpenVotingSessionInput.class)))
            .thenThrow(new VotingSessionAlreadyOpenedException(agendaId));

        mockMvc.perform(post("/pautas/{id}/sessao", AGENDA_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Esta pauta já possui uma sessão de votação."))
            .andExpect(jsonPath("$.error.code").value("VOTING_SESSION_ALREADY_OPENED"));
    }

    @Test
    void shouldReturnConflictWhenVotingSessionIsClosed() throws Exception {
        AgendaId agendaId = AgendaId.from(UUID.fromString(AGENDA_ID));
        when(registerVoteUseCase.execute(any(RegisterVoteInput.class)))
            .thenThrow(new VotingSessionClosedException(agendaId));

        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "SIM"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("A sessão de votação não está aberta para esta pauta."))
            .andExpect(jsonPath("$.error.code").value("VOTING_SESSION_CLOSED"));
    }

    @Test
    void shouldReturnConflictWhenVoteIsDuplicated() throws Exception {
        AgendaId agendaId = AgendaId.from(UUID.fromString(AGENDA_ID));
        AssociateId associateId = AssociateId.from("associado-123");
        when(registerVoteUseCase.execute(any(RegisterVoteInput.class)))
            .thenThrow(new DuplicateVoteException(agendaId, associateId));

        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "SIM"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Este associado já votou nesta pauta."))
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_VOTE"));
    }

    @Test
    void shouldReturnForbiddenWhenAssociateIsUnableToVote() throws Exception {
        when(registerVoteUseCase.execute(any(RegisterVoteInput.class)))
            .thenThrow(new AssociateUnableToVoteException(AssociateId.from("associado-123")));

        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "SIM"
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Este associado não está apto a votar."))
            .andExpect(jsonPath("$.error.code").value("ASSOCIATE_UNABLE_TO_VOTE"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenAssociateEligibilityServiceIsUnavailable() throws Exception {
        when(registerVoteUseCase.execute(any(RegisterVoteInput.class)))
            .thenThrow(new AssociateEligibilityUnavailableException("User-info service is unavailable", null));

        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "SIM"
                ))))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("Não foi possível consultar se o associado está apto a votar. Tente novamente em instantes."))
            .andExpect(jsonPath("$.error.code").value("ASSOCIATE_ELIGIBILITY_UNAVAILABLE"));
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/pautas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("O corpo da requisição está inválido ou mal formatado."))
            .andExpect(jsonPath("$.error.code").value("MALFORMED_JSON"))
            .andExpect(jsonPath("$.error.details[0].field").value("body"));
    }

    @Test
    void shouldReturnBadRequestWhenDomainInputIsInvalid() throws Exception {
        when(findVotingResultUseCase.execute(any(FindVotingResultInput.class)))
            .thenThrow(new InvalidDomainObjectException("agendaId must be a valid UUID"));

        mockMvc.perform(get("/pautas/{id}/resultado", "invalid-id"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Dados inválidos para a operação."))
            .andExpect(jsonPath("$.error.code").value("INVALID_DOMAIN_OBJECT"))
            .andExpect(jsonPath("$.error.details[0].message").value("Identificador da pauta inválido."));
    }

    @Test
    void shouldCallUseCaseWithMappedRegisterVoteInput() throws Exception {
        RegisterVoteOutput output = new RegisterVoteOutput(VOTE_ID, AGENDA_ID, "associado-123", VoteChoice.NO, NOW);
        when(registerVoteUseCase.execute(any(RegisterVoteInput.class))).thenReturn(output);

        mockMvc.perform(post("/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterVoteRequest(
                    AGENDA_ID,
                    "associado-123",
                    "529.982.247-25",
                    "NAO"
                ))))
            .andExpect(status().isCreated());

        verify(registerVoteUseCase).execute(new RegisterVoteInput(
            AGENDA_ID,
            "associado-123",
            "529.982.247-25",
            "NAO"
        ));
    }
}
