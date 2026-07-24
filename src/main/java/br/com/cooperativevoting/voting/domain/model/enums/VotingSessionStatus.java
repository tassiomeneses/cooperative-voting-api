package br.com.cooperativevoting.voting.domain.model.enums;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Instant;

public enum VotingSessionStatus {
    OPEN,
    CLOSED;

    public static VotingSessionStatus at(Instant openedAt, Instant closedAt, Instant referenceTime) {
        DomainValidator.notNull(openedAt, "openedAt");
        DomainValidator.notNull(closedAt, "closedAt");
        DomainValidator.notNull(referenceTime, "referenceTime");

        boolean isOpen = !referenceTime.isBefore(openedAt) && referenceTime.isBefore(closedAt);
        return isOpen ? OPEN : CLOSED;
    }
}
