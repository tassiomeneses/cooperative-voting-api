package br.com.cooperativevoting.voting.domain.exception;

import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;

public class VotingSessionClosedException extends DomainException {

    public VotingSessionClosedException(AgendaId agendaId) {
        super("VOTING_SESSION_CLOSED", "Voting session is closed for agenda: " + agendaId);
    }

    public static VotingSessionClosedException noSessionFor(AgendaId agendaId) {
        return new VotingSessionClosedException(agendaId);
    }
}
