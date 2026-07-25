package br.com.cooperativevoting.voting.adapter.in.mobile;

import br.com.cooperativevoting.voting.adapter.in.mobile.dto.MobileScreenButtonResponse;
import br.com.cooperativevoting.voting.adapter.in.mobile.dto.MobileScreenItemResponse;
import br.com.cooperativevoting.voting.adapter.in.mobile.dto.MobileScreenResponse;
import br.com.cooperativevoting.voting.adapter.in.mobile.dto.MobileScreenType;
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
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/v1/mobile")
@Tag(name = "Mobile", description = "Contrato de telas dinamicas consumido pelo aplicativo mobile.")
public class MobileVotingScreenController {

    private final CreateAgendaUseCase createAgendaUseCase;
    private final OpenVotingSessionUseCase openVotingSessionUseCase;
    private final RegisterVoteUseCase registerVoteUseCase;
    private final FindVotingResultUseCase findVotingResultUseCase;
    private final AgendaRestMapper agendaRestMapper;
    private final VotingSessionRestMapper votingSessionRestMapper;
    private final VoteRestMapper voteRestMapper;
    private final VotingResultRestMapper votingResultRestMapper;
    private final MobileCallbackUrlBuilder urlBuilder;

    public MobileVotingScreenController(
        CreateAgendaUseCase createAgendaUseCase,
        OpenVotingSessionUseCase openVotingSessionUseCase,
        RegisterVoteUseCase registerVoteUseCase,
        FindVotingResultUseCase findVotingResultUseCase,
        AgendaRestMapper agendaRestMapper,
        VotingSessionRestMapper votingSessionRestMapper,
        VoteRestMapper voteRestMapper,
        VotingResultRestMapper votingResultRestMapper,
        MobileCallbackUrlBuilder urlBuilder
    ) {
        this.createAgendaUseCase = createAgendaUseCase;
        this.openVotingSessionUseCase = openVotingSessionUseCase;
        this.registerVoteUseCase = registerVoteUseCase;
        this.findVotingResultUseCase = findVotingResultUseCase;
        this.agendaRestMapper = agendaRestMapper;
        this.votingSessionRestMapper = votingSessionRestMapper;
        this.voteRestMapper = voteRestMapper;
        this.votingResultRestMapper = votingResultRestMapper;
        this.urlBuilder = urlBuilder;
    }

    @PostMapping("/pautas/nova")
    @Operation(summary = "Tela para cadastrar pauta", description = "Retorna uma tela FORMULARIO conforme o Anexo 1.")
    public MobileScreenResponse newAgendaScreen() {
        return form(
            "Cadastrar pauta",
            List.of(
                MobileScreenItemResponse.inputText("titulo", "Titulo da pauta", ""),
                MobileScreenItemResponse.inputText("descricao", "Descricao da pauta", "")
            ),
            button("Cadastrar", "/v1/mobile/pautas", Map.of()),
            button("Cancelar", "/v1/mobile/pautas/nova", Map.of())
        );
    }

    @PostMapping("/pautas")
    @Operation(summary = "Cadastrar pauta pelo fluxo mobile", description = "Executa o cadastro e retorna a proxima tela no contrato mobile.")
    public ResponseEntity<MobileScreenResponse> createAgenda(@Valid @RequestBody CreateAgendaRequest request) {
        CreateAgendaOutput output = createAgendaUseCase.execute(agendaRestMapper.toInput(request));

        MobileScreenResponse response = form(
            "Pauta cadastrada",
            List.of(MobileScreenItemResponse.text(
                "Pauta cadastrada com sucesso. Id: " + output.agendaId()
            )),
            button("Abrir sessao", "/v1/mobile/pautas/" + output.agendaId() + "/sessao/nova", Map.of()),
            button("Cadastrar outra", "/v1/mobile/pautas/nova", Map.of())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pautas/{id}/sessao/nova")
    @Operation(summary = "Tela para abrir sessao", description = "Retorna formulario de duracao da sessao. O valor padrao de negocio e 1 minuto.")
    public MobileScreenResponse newVotingSessionScreen(@PathVariable String id) {
        return form(
            "Abrir sessao de votacao",
            List.of(
                MobileScreenItemResponse.text("Informe a duracao em minutos. Se nao informar, a sessao ficara aberta por 1 minuto."),
                MobileScreenItemResponse.inputNumber("duracaoMinutos", "Duracao em minutos", 1)
            ),
            button("Abrir sessao", "/v1/mobile/pautas/" + id + "/sessao", Map.of()),
            button("Cancelar", "/v1/mobile/pautas/nova", Map.of())
        );
    }

    @PostMapping("/pautas/{id}/sessao")
    @Operation(summary = "Abrir sessao pelo fluxo mobile", description = "Executa a abertura da sessao e retorna uma tela de confirmacao.")
    public ResponseEntity<MobileScreenResponse> openVotingSession(
        @PathVariable String id,
        @Valid @RequestBody(required = false) OpenVotingSessionRequest request
    ) {
        OpenVotingSessionOutput output = openVotingSessionUseCase.execute(votingSessionRestMapper.toInput(id, request));

        MobileScreenResponse response = form(
            "Sessao aberta",
            List.of(MobileScreenItemResponse.text(
                "Sessao aberta ate " + output.closedAt() + ". Duracao: " + minutesBetween(output) + " minuto(s)."
            )),
            button("Votar", "/v1/mobile/pautas/" + output.agendaId() + "/voto/identificacao", Map.of()),
            button("Resultado", "/v1/mobile/pautas/" + output.agendaId() + "/resultado", Map.of())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pautas/{id}/voto/identificacao")
    @Operation(summary = "Tela para identificar associado", description = "Coleta associado e CPF antes da selecao SIM/NAO.")
    public MobileScreenResponse voteIdentificationScreen(@PathVariable String id) {
        return form(
            "Identificar associado",
            List.of(
                MobileScreenItemResponse.inputText("associadoId", "Identificador do associado", ""),
                MobileScreenItemResponse.inputText("cpf", "CPF do associado", "")
            ),
            button("Continuar", "/v1/mobile/pautas/" + id + "/voto/opcoes", Map.of("pautaId", id)),
            button("Resultado", "/v1/mobile/pautas/" + id + "/resultado", Map.of())
        );
    }

    @PostMapping("/pautas/{id}/voto/opcoes")
    @Operation(summary = "Tela de selecao de voto", description = "Retorna lista SELECAO com as opcoes Sim e Nao.")
    public MobileScreenResponse voteSelectionScreen(
        @PathVariable String id,
        @Valid @RequestBody MobileVoteIdentificationRequest request
    ) {
        return selection(
            "Escolha seu voto",
            List.of(
                MobileScreenItemResponse.selection("Sim", urlBuilder.path("/v1/mobile/votos"), voteBody(id, request, "SIM")),
                MobileScreenItemResponse.selection("Nao", urlBuilder.path("/v1/mobile/votos"), voteBody(id, request, "NAO"))
            )
        );
    }

    @PostMapping("/votos")
    @Operation(summary = "Registrar voto pelo fluxo mobile", description = "Executa o voto e retorna uma tela de confirmacao.")
    public ResponseEntity<MobileScreenResponse> registerVote(@Valid @RequestBody RegisterVoteRequest request) {
        RegisterVoteOutput output = registerVoteUseCase.execute(voteRestMapper.toInput(request));

        MobileScreenResponse response = form(
            "Voto registrado",
            List.of(MobileScreenItemResponse.text(
                "Voto registrado com sucesso. Id: " + output.voteId()
            )),
            button("Resultado", "/v1/mobile/pautas/" + output.agendaId() + "/resultado", Map.of()),
            button("Novo voto", "/v1/mobile/pautas/" + output.agendaId() + "/voto/identificacao", Map.of())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pautas/{id}/resultado")
    @Operation(summary = "Tela de resultado", description = "Retorna o resultado da pauta como tela FORMULARIO.")
    public MobileScreenResponse votingResultScreen(@PathVariable String id) {
        FindVotingResultOutput output = findVotingResultUseCase.execute(votingResultRestMapper.toInput(id));

        return form(
            "Resultado da votacao",
            List.of(MobileScreenItemResponse.text(
                "Resultado: " + outcome(output)
                    + "\nSim: " + output.yesVotes()
                    + "\nNao: " + output.noVotes()
                    + "\nTotal: " + output.totalVotes()
            )),
            button("Votar", "/v1/mobile/pautas/" + output.agendaId() + "/voto/identificacao", Map.of()),
            button("Nova pauta", "/v1/mobile/pautas/nova", Map.of())
        );
    }

    private MobileScreenResponse form(
        String title,
        List<MobileScreenItemResponse> items,
        MobileScreenButtonResponse okButton,
        MobileScreenButtonResponse cancelButton
    ) {
        return new MobileScreenResponse(MobileScreenType.FORMULARIO, title, items, okButton, cancelButton);
    }

    private MobileScreenResponse selection(String title, List<MobileScreenItemResponse> items) {
        return new MobileScreenResponse(MobileScreenType.SELECAO, title, items, null, null);
    }

    private MobileScreenButtonResponse button(String text, String path, Map<String, Object> body) {
        return new MobileScreenButtonResponse(text, urlBuilder.path(path), body);
    }

    private Map<String, Object> voteBody(String agendaId, MobileVoteIdentificationRequest request, String vote) {
        return Map.of(
            "pautaId", agendaId,
            "associadoId", request.associadoId(),
            "cpf", request.cpf(),
            "voto", vote
        );
    }

    private long minutesBetween(OpenVotingSessionOutput output) {
        return Duration.between(output.openedAt(), output.closedAt()).toMinutes();
    }

    private String outcome(FindVotingResultOutput output) {
        return switch (output.outcome()) {
            case APPROVED -> "APROVADA";
            case REJECTED -> "REJEITADA";
            case TIED -> "EMPATADA";
        };
    }
}
