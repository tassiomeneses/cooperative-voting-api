package br.com.cooperativevoting.voting.domain.validation;

import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;

import java.util.UUID;

public final class DomainValidator {

    private DomainValidator() {
    }

    public static <T> T notNull(T value, String fieldName) {
        if (value == null) {
            throw new InvalidDomainObjectException(fieldName + " must not be null");
        }
        return value;
    }

    public static String notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainObjectException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public static void maxLength(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new InvalidDomainObjectException(fieldName + " must have at most " + maxLength + " characters");
        }
    }

    public static long notNegative(long value, String fieldName) {
        if (value < 0) {
            throw new InvalidDomainObjectException(fieldName + " must not be negative");
        }
        return value;
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidDomainObjectException(message);
        }
    }

    public static UUID uuidFrom(String value, String fieldName) {
        try {
            return UUID.fromString(notBlank(value, fieldName));
        } catch (IllegalArgumentException exception) {
            throw new InvalidDomainObjectException(fieldName + " must be a valid UUID");
        }
    }
}
