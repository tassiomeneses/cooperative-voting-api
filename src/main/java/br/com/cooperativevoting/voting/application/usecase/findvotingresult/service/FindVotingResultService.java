package br.com.cooperativevoting.voting.application.usecase.findvotingresult.service;

import br.com.cooperativevoting.voting.application.port.in.FindVotingResultUseCase;
import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.port.out.VoteTally;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.mapper.FindVotingResultMapper;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.model.VotingResult;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

public class FindVotingResultService implements FindVotingResultUseCase {

    private final AgendaRepositoryPort agendaRepositoryPort;
    private final FindVotingResultMapper mapper;

    public FindVotingResultService(
        AgendaRepositoryPort agendaRepositoryPort,
        FindVotingResultMapper mapper
    ) {
        this.agendaRepositoryPort = DomainValidator.notNull(agendaRepositoryPort, "agendaRepositoryPort");
        this.mapper = DomainValidator.notNull(mapper, "findVotingResultMapper");
    }

    @Override
    public FindVotingResultOutput execute(FindVotingResultInput input) {
        DomainValidator.notNull(input, "input");

        AgendaId agendaId = mapper.toAgendaId(input);
        VoteTally tally = agendaRepositoryPort.findVoteTallyById(agendaId)
            .orElseThrow(() -> new AgendaNotFoundException(agendaId));
        VotingResult result = mapper.toDomain(agendaId, tally);

        return mapper.toOutput(result);
    }
}
