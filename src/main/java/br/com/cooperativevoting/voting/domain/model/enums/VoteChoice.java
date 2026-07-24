package br.com.cooperativevoting.voting.domain.model.enums;

import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;

import java.text.Normalizer;
import java.util.Locale;

public enum VoteChoice {
    YES,
    NO;

    public static VoteChoice from(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "SIM", "S", "YES", "Y" -> YES;
            case "NAO", "N", "NO" -> NO;
            default -> throw new InvalidDomainObjectException("voteChoice must be YES or NO");
        };
    }

    public boolean isYes() {
        return this == YES;
    }

    public boolean isNo() {
        return this == NO;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainObjectException("voteChoice must not be blank");
        }

        String withoutAccents = Normalizer
            .normalize(value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

        return withoutAccents.toUpperCase(Locale.ROOT);
    }
}
