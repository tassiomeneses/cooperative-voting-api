package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.util.UUID;

public record VotingSessionId(UUID value) {

    public VotingSessionId {
        value = DomainValidator.notNull(value, "votingSessionId");
    }

    public static VotingSessionId newId() {
        return new VotingSessionId(UUID.randomUUID());
    }

    public static VotingSessionId from(UUID value) {
        return new VotingSessionId(value);
    }

    public static VotingSessionId from(String value) {
        return new VotingSessionId(DomainValidator.uuidFrom(value, "votingSessionId"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
