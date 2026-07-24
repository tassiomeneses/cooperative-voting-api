package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

public record AgendaTitle(String value) {

    private static final int MAX_LENGTH = 120;

    public AgendaTitle {
        value = DomainValidator.notBlank(value, "agendaTitle");
        DomainValidator.maxLength(value, "agendaTitle", MAX_LENGTH);
    }

    public static AgendaTitle from(String value) {
        return new AgendaTitle(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
