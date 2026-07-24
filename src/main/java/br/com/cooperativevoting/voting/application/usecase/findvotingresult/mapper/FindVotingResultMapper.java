package br.com.cooperativevoting.voting.application.usecase.findvotingresult.mapper;

import br.com.cooperativevoting.voting.application.port.out.VoteTally;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.domain.factory.VotingResultFactory;
import br.com.cooperativevoting.voting.domain.model.VotingResult;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;

public class FindVotingResultMapper {

    public AgendaId toAgendaId(FindVotingResultInput input) {
        return AgendaId.from(input.agendaId());
    }

    public VotingResult toDomain(AgendaId agendaId, VoteTally tally) {
        return VotingResultFactory.fromTally(agendaId, tally.yesVotes(), tally.noVotes());
    }

    public FindVotingResultOutput toOutput(VotingResult result) {
        return new FindVotingResultOutput(
            result.getAgendaId().toString(),
            result.getYesVotes(),
            result.getNoVotes(),
            result.totalVotes(),
            result.outcome()
        );
    }
}
