package br.com.cooperativevoting.voting.adapter.out.persistence;

import org.hibernate.exception.ConstraintViolationException;

final class ConstraintViolationDetector {

    private ConstraintViolationDetector() {
    }

    static boolean hasConstraint(Throwable throwable, String... constraintNames) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                && matches(constraintViolationException.getConstraintName(), constraintNames)) {
                return true;
            }

            if (current.getMessage() != null && matchesMessage(current.getMessage(), constraintNames)) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private static boolean matches(String actual, String... expectedNames) {
        if (actual == null) {
            return false;
        }

        for (String expectedName : expectedNames) {
            if (actual.equals(expectedName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesMessage(String message, String... expectedNames) {
        for (String expectedName : expectedNames) {
            if (message.contains(expectedName)) {
                return true;
            }
        }
        return false;
    }
}
