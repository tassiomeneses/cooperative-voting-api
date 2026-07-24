package br.com.cooperativevoting.voting.application.usecase.registervote.service;

import br.com.cooperativevoting.voting.application.port.in.RegisterVoteUseCase;
import br.com.cooperativevoting.voting.application.port.out.AssociateEligibilityPort;
import br.com.cooperativevoting.voting.application.port.out.VoteRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import br.com.cooperativevoting.voting.application.usecase.registervote.mapper.RegisterVoteMapper;
import br.com.cooperativevoting.voting.domain.exception.AssociateUnableToVoteException;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.enums.AssociateVotingStatus;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import br.com.cooperativevoting.voting.domain.model.vo.VoteId;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Clock;
import java.time.Instant;

public class RegisterVoteService implements RegisterVoteUseCase {

    private final VoteRepositoryPort voteRepositoryPort;
    private final AssociateEligibilityPort associateEligibilityPort;
    private final RegisterVoteMapper mapper;
    private final Clock clock;

    public RegisterVoteService(
        VoteRepositoryPort voteRepositoryPort,
        AssociateEligibilityPort associateEligibilityPort,
        RegisterVoteMapper mapper,
        Clock clock
    ) {
        this.voteRepositoryPort = DomainValidator.notNull(voteRepositoryPort, "voteRepositoryPort");
        this.associateEligibilityPort = DomainValidator.notNull(associateEligibilityPort, "associateEligibilityPort");
        this.mapper = DomainValidator.notNull(mapper, "registerVoteMapper");
        this.clock = DomainValidator.notNull(clock, "clock");
    }

    @Override
    public RegisterVoteOutput execute(RegisterVoteInput input) {
        DomainValidator.notNull(input, "input");

        AgendaId agendaId = mapper.toAgendaId(input);
        AssociateId associateId = mapper.toAssociateId(input);
        Cpf cpf = mapper.toCpf(input);
        VoteChoice choice = mapper.toChoice(input);

        AssociateVotingStatus votingStatus = associateEligibilityPort.check(cpf);
        if (!DomainValidator.notNull(votingStatus, "associateVotingStatus").canVote()) {
            throw new AssociateUnableToVoteException(associateId);
        }

        Instant castAt = Instant.now(clock);
        Vote vote = Vote.cast(VoteId.newId(), agendaId, associateId, choice, castAt);
        Vote savedVote = voteRepositoryPort.saveIfSessionOpen(vote, cpf);

        return mapper.toOutput(savedVote);
    }
}
