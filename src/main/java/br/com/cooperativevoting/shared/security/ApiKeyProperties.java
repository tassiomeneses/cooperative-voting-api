package br.com.cooperativevoting.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.api-key")
public record ApiKeyProperties(
    boolean enabled,
    String headerName,
    String value
) {

    public ApiKeyProperties {
        headerName = headerName == null || headerName.isBlank() ? "X-API-Key" : headerName.trim();
    }

    boolean hasConfiguredValue() {
        return value != null && !value.isBlank();
    }
}
