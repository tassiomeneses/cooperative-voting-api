package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.util.UUID;

public record VoteId(UUID value) {

    public VoteId {
        value = DomainValidator.notNull(value, "voteId");
    }

    public static VoteId newId() {
        return new VoteId(UUID.randomUUID());
    }

    public static VoteId from(UUID value) {
        return new VoteId(value);
    }

    public static VoteId from(String value) {
        return new VoteId(DomainValidator.uuidFrom(value, "voteId"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
