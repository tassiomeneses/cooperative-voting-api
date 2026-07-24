package br.com.cooperativevoting.voting.adapter.in.rest.mapper;

import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.OpenVotingSessionRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.OpenVotingSessionResponse;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;
import org.springframework.stereotype.Component;

@Component
public class VotingSessionRestMapper {

    public OpenVotingSessionInput toInput(String agendaId, OpenVotingSessionRequest request) {
        Long durationMinutes = request == null ? null : request.duracaoMinutos();
        return new OpenVotingSessionInput(agendaId, durationMinutes);
    }

    public OpenVotingSessionResponse toResponse(OpenVotingSessionOutput output) {
        return new OpenVotingSessionResponse(
            output.agendaId(),
            output.votingSessionId(),
            output.openedAt(),
            output.closedAt(),
            toPortugueseStatus(output.status())
        );
    }

    private String toPortugueseStatus(VotingSessionStatus status) {
        return switch (status) {
            case OPEN -> "ABERTA";
            case CLOSED -> "FECHADA";
        };
    }
}
