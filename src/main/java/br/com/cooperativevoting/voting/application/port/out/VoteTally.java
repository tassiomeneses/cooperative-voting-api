package br.com.cooperativevoting.voting.application.port.out;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

public record VoteTally(long yesVotes, long noVotes) {

    public VoteTally {
        yesVotes = DomainValidator.notNegative(yesVotes, "yesVotes");
        noVotes = DomainValidator.notNegative(noVotes, "noVotes");
    }
}
