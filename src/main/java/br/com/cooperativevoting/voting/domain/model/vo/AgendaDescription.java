package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

public record AgendaDescription(String value) {

    private static final int MAX_LENGTH = 500;

    public AgendaDescription {
        value = value == null ? "" : value.trim();
        DomainValidator.maxLength(value, "agendaDescription", MAX_LENGTH);
    }

    public static AgendaDescription from(String value) {
        return new AgendaDescription(value);
    }

    public static AgendaDescription empty() {
        return new AgendaDescription("");
    }

    public boolean isEmpty() {
        return value.isBlank();
    }

    @Override
    public String toString() {
        return value;
    }
}
