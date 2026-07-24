package br.com.cooperativevoting.voting.application.port.in;

import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;

public interface FindVotingResultUseCase {

    FindVotingResultOutput execute(FindVotingResultInput input);
}
