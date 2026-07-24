package br.com.cooperativevoting.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.identity-hash")
public record IdentityHashProperties(
    String pepper,
    boolean requireCustomPepper
) {

    public static final String DEFAULT_PEPPER = "local-development-pepper-change-me";

    public IdentityHashProperties {
        pepper = pepper == null || pepper.isBlank() ? DEFAULT_PEPPER : pepper;
    }
}
