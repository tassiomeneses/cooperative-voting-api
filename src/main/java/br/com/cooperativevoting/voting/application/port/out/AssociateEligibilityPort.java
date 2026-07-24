package br.com.cooperativevoting.voting.application.port.out;

import br.com.cooperativevoting.voting.domain.model.enums.AssociateVotingStatus;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;

public interface AssociateEligibilityPort {

    AssociateVotingStatus check(Cpf cpf);
}
