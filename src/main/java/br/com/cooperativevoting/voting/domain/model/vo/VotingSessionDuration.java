package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Duration;

public record VotingSessionDuration(Duration value) {

    public static final Duration MAX_DURATION = Duration.ofHours(24);
    public static final VotingSessionDuration DEFAULT = new VotingSessionDuration(Duration.ofMinutes(1));

    public VotingSessionDuration {
        value = DomainValidator.notNull(value, "votingSessionDuration");
        DomainValidator.require(!value.isZero() && !value.isNegative(), "votingSessionDuration must be positive");
        DomainValidator.require(
            value.compareTo(MAX_DURATION) <= 0,
            "votingSessionDuration must be at most 24 hours"
        );
    }

    public static VotingSessionDuration from(Duration value) {
        return new VotingSessionDuration(value);
    }

    public static VotingSessionDuration fromMinutes(long minutes) {
        return new VotingSessionDuration(Duration.ofMinutes(minutes));
    }

    public boolean isDefault() {
        return DEFAULT.equals(this);
    }
}
