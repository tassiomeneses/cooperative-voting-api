package br.com.cooperativevoting.voting.domain.exception;

public class InvalidDomainObjectException extends DomainException {

    public InvalidDomainObjectException(String message) {
        super("INVALID_DOMAIN_OBJECT", message);
    }
}
