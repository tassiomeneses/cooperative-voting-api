package br.com.cooperativevoting.voting.domain.exception;

import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;

public class DuplicateVoteException extends DomainException {

    public DuplicateVoteException(AgendaId agendaId, AssociateId associateId) {
        super(
            "DUPLICATE_VOTE",
            "Associate " + associateId + " has already voted for agenda: " + agendaId
        );
    }
}
