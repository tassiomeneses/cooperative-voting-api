package br.com.cooperativevoting.voting.domain.factory;

import br.com.cooperativevoting.voting.domain.model.VotingResult;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;

public final class VotingResultFactory {

    private VotingResultFactory() {
    }

    public static VotingResult fromTally(AgendaId agendaId, long yesVotes, long noVotes) {
        return VotingResult.of(agendaId, yesVotes, noVotes);
    }
}
