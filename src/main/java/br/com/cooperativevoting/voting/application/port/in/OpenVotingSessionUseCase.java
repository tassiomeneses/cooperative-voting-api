package br.com.cooperativevoting.voting.application.port.in;

import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;

public interface OpenVotingSessionUseCase {

    OpenVotingSessionOutput execute(OpenVotingSessionInput input);
}
