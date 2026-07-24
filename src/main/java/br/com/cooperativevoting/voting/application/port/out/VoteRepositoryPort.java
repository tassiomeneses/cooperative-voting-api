package br.com.cooperativevoting.voting.application.port.out;

import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;

public interface VoteRepositoryPort {

    Vote saveIfSessionOpen(Vote vote, Cpf cpf);

}
