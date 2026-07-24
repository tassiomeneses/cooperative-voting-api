package br.com.cooperativevoting.voting.adapter.in.rest.mapper;

import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.VotingResultResponse;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;
import org.springframework.stereotype.Component;

@Component
public class VotingResultRestMapper {

    public FindVotingResultInput toInput(String agendaId) {
        return new FindVotingResultInput(agendaId);
    }

    public VotingResultResponse toResponse(FindVotingResultOutput output) {
        return new VotingResultResponse(
            output.agendaId(),
            output.yesVotes(),
            output.noVotes(),
            output.totalVotes(),
            toPortugueseOutcome(output.outcome())
        );
    }

    private String toPortugueseOutcome(VotingResultOutcome outcome) {
        return switch (outcome) {
            case APPROVED -> "APROVADA";
            case REJECTED -> "REJEITADA";
            case TIED -> "EMPATADA";
        };
    }
}
