package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.util.UUID;

public record AgendaId(UUID value) {

    public AgendaId {
        value = DomainValidator.notNull(value, "agendaId");
    }

    public static AgendaId newId() {
        return new AgendaId(UUID.randomUUID());
    }

    public static AgendaId from(UUID value) {
        return new AgendaId(value);
    }

    public static AgendaId from(String value) {
        return new AgendaId(DomainValidator.uuidFrom(value, "agendaId"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
