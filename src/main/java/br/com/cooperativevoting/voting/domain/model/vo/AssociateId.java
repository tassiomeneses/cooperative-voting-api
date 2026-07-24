package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

public record AssociateId(String value) {

    private static final int MAX_LENGTH = 64;

    public AssociateId {
        value = DomainValidator.notBlank(value, "associateId");
        DomainValidator.maxLength(value, "associateId", MAX_LENGTH);
    }

    public static AssociateId from(String value) {
        return new AssociateId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
