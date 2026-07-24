package br.com.cooperativevoting.voting.application.port.in;

import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;

public interface RegisterVoteUseCase {

    RegisterVoteOutput execute(RegisterVoteInput input);
}
