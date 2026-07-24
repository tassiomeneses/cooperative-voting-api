package br.com.cooperativevoting.voting.adapter.in.rest.mapper;

import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.RegisterVoteRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.RegisterVoteResponse;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import org.springframework.stereotype.Component;

@Component
public class VoteRestMapper {

    public RegisterVoteInput toInput(RegisterVoteRequest request) {
        return new RegisterVoteInput(
            request.pautaId(),
            request.associadoId(),
            request.cpf(),
            request.voto()
        );
    }

    public RegisterVoteResponse toResponse(RegisterVoteOutput output) {
        return new RegisterVoteResponse(
            output.voteId(),
            output.agendaId(),
            output.associateId(),
            toPortugueseVote(output.choice()),
            output.castAt()
        );
    }

    private String toPortugueseVote(VoteChoice choice) {
        return switch (choice) {
            case YES -> "SIM";
            case NO -> "NAO";
        };
    }
}
