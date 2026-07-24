package br.com.cooperativevoting.voting.application.usecase.registervote.mapper;

import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;

public class RegisterVoteMapper {

    public AgendaId toAgendaId(RegisterVoteInput input) {
        return AgendaId.from(input.agendaId());
    }

    public AssociateId toAssociateId(RegisterVoteInput input) {
        return AssociateId.from(input.associateId());
    }

    public Cpf toCpf(RegisterVoteInput input) {
        return Cpf.from(input.cpf());
    }

    public VoteChoice toChoice(RegisterVoteInput input) {
        return VoteChoice.from(input.choice());
    }

    public RegisterVoteOutput toOutput(Vote vote) {
        return new RegisterVoteOutput(
            vote.getId().toString(),
            vote.getAgendaId().toString(),
            vote.getAssociateId().value(),
            vote.getChoice(),
            vote.getCastAt()
        );
    }
}
