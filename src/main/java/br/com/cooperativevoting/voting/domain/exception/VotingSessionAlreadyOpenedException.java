package br.com.cooperativevoting.voting.domain.exception;

import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;

public class VotingSessionAlreadyOpenedException extends DomainException {

    public VotingSessionAlreadyOpenedException(AgendaId agendaId) {
        super("VOTING_SESSION_ALREADY_OPENED", "Agenda already has a voting session: " + agendaId);
    }
}
