package br.com.cooperativevoting.voting.adapter.in.rest;

import br.com.cooperativevoting.shared.web.ApiResponse;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.RegisterVoteRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.RegisterVoteResponse;
import br.com.cooperativevoting.voting.adapter.in.rest.mapper.VoteRestMapper;
import br.com.cooperativevoting.voting.application.port.in.RegisterVoteUseCase;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequestMapping({"/votos", "/v1/votos"})
@Tag(name = "Votos", description = "Operações para registro de votos dos associados.")
public class VoteRestController {

    private final RegisterVoteUseCase registerVoteUseCase;
    private final VoteRestMapper voteRestMapper;

    public VoteRestController(RegisterVoteUseCase registerVoteUseCase, VoteRestMapper voteRestMapper) {
        this.registerVoteUseCase = registerVoteUseCase;
        this.voteRestMapper = voteRestMapper;
    }

    @PostMapping
    @Operation(summary = "Registrar voto", description = "Registra o voto de um associado em uma pauta com sessão aberta.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Voto registrado."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Requisição inválida.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Associado não apto a votar.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Pauta não encontrada.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Voto duplicado ou sessão fechada.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "Serviço externo de elegibilidade indisponível.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<RegisterVoteResponse>> registerVote(
        @Valid @RequestBody RegisterVoteRequest request,
        HttpServletRequest servletRequest
    ) {
        RegisterVoteOutput output = registerVoteUseCase.execute(voteRestMapper.toInput(request));
        RegisterVoteResponse response = voteRestMapper.toResponse(output);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(ApiResponse.success("Voto registrado com sucesso.", response, servletRequest.getRequestURI()));
    }
}
