package br.com.cooperativevoting.voting.adapter.in.rest;

import br.com.cooperativevoting.shared.web.ApiResponse;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.CreateAgendaRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.OpenVotingSessionRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.CreateAgendaResponse;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.OpenVotingSessionResponse;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.VotingResultResponse;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.AgendaRestMapper;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.VotingResultRestMapper;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.VotingSessionRestMapper;
import br.com.cooperativevoting.voting.application.port.in.CreateAgendaUseCase;
import br.com.cooperativevoting.voting.application.port.in.FindVotingResultUseCase;
import br.com.cooperativevoting.voting.application.port.in.OpenVotingSessionUseCase;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequestMapping({"/pautas", "/v1/pautas"})
@Tag(name = "Pautas", description = "Operações para cadastro de pautas, abertura de sessão e consulta de resultado.")
public class AgendaRestController {

    private final CreateAgendaUseCase createAgendaUseCase;
    private final OpenVotingSessionUseCase openVotingSessionUseCase;
    private final FindVotingResultUseCase findVotingResultUseCase;
    private final AgendaRestMapper agendaRestMapper;
    private final VotingSessionRestMapper votingSessionRestMapper;
    private final VotingResultRestMapper votingResultRestMapper;

    public AgendaRestController(
        CreateAgendaUseCase createAgendaUseCase,
        OpenVotingSessionUseCase openVotingSessionUseCase,
        FindVotingResultUseCase findVotingResultUseCase,
        AgendaRestMapper agendaRestMapper,
        VotingSessionRestMapper votingSessionRestMapper,
        VotingResultRestMapper votingResultRestMapper
    ) {
        this.createAgendaUseCase = createAgendaUseCase;
        this.openVotingSessionUseCase = openVotingSessionUseCase;
        this.findVotingResultUseCase = findVotingResultUseCase;
        this.agendaRestMapper = agendaRestMapper;
        this.votingSessionRestMapper = votingSessionRestMapper;
        this.votingResultRestMapper = votingResultRestMapper;
    }

    @PostMapping
    @Operation(summary = "Cadastrar pauta", description = "Cadastra uma nova pauta para votação.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pauta cadastrada."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Requisição inválida.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<CreateAgendaResponse>> createAgenda(
        @Valid @RequestBody CreateAgendaRequest request,
        HttpServletRequest servletRequest
    ) {
        CreateAgendaOutput output = createAgendaUseCase.execute(agendaRestMapper.toInput(request));
        CreateAgendaResponse response = agendaRestMapper.toResponse(output);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(ApiResponse.success("Pauta cadastrada com sucesso.", response, servletRequest.getRequestURI()));
    }

    @PostMapping("/{id}/sessao")
    @Operation(summary = "Abrir sessão de votação", description = "Abre uma sessão de votação para uma pauta existente.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Sessão aberta."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Pauta não encontrada.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Sessão já existente para a pauta.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<OpenVotingSessionResponse>> openVotingSession(
        @PathVariable String id,
        @Valid @RequestBody(required = false) OpenVotingSessionRequest request,
        HttpServletRequest servletRequest
    ) {
        OpenVotingSessionOutput output = openVotingSessionUseCase.execute(votingSessionRestMapper.toInput(id, request));
        OpenVotingSessionResponse response = votingSessionRestMapper.toResponse(output);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .build()
            .toUri();

        return ResponseEntity
            .created(location)
            .body(ApiResponse.success("Sessão de votação aberta com sucesso.", response, servletRequest.getRequestURI()));
    }

    @GetMapping("/{id}/resultado")
    @Operation(summary = "Buscar resultado", description = "Retorna a totalização dos votos de uma pauta.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resultado encontrado."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Pauta não encontrada.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<VotingResultResponse>> findVotingResult(
        @PathVariable String id,
        HttpServletRequest servletRequest
    ) {
        FindVotingResultOutput output = findVotingResultUseCase.execute(votingResultRestMapper.toInput(id));
        VotingResultResponse response = votingResultRestMapper.toResponse(output);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success("Resultado da votação encontrado com sucesso.", response, servletRequest.getRequestURI()));
    }
}
