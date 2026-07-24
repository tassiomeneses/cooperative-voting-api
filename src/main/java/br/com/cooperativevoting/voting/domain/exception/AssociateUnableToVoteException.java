package br.com.cooperativevoting.voting.domain.exception;

import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;

public class AssociateUnableToVoteException extends DomainException {

    public AssociateUnableToVoteException(AssociateId associateId) {
        super("ASSOCIATE_UNABLE_TO_VOTE", "Associate is unable to vote: " + associateId);
    }
}
