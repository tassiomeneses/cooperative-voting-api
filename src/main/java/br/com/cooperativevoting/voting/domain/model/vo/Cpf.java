package br.com.cooperativevoting.voting.domain.model.vo;

import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

public record Cpf(String value) {

    private static final int CPF_LENGTH = 11;

    public Cpf {
        value = normalize(value);
        DomainValidator.require(value.length() == CPF_LENGTH, "cpf must have 11 digits");
        DomainValidator.require(isValidCpf(value), "cpf must be valid");
    }

    public static Cpf from(String value) {
        return new Cpf(value);
    }

    public String masked() {
        return value.substring(0, 3) + ".***.***-" + value.substring(9);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        return rawValue.replaceAll("\\D", "");
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateDigit(cpf, 9);
        int secondDigit = calculateDigit(cpf, 10);

        return Character.getNumericValue(cpf.charAt(9)) == firstDigit
            && Character.getNumericValue(cpf.charAt(10)) == secondDigit;
    }

    private static int calculateDigit(String cpf, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(cpf.charAt(index)) * (length + 1 - index);
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Override
    public String toString() {
        return masked();
    }
}
