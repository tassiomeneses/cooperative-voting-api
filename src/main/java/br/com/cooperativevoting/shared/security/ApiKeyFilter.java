package br.com.cooperativevoting.shared.security;

import br.com.cooperativevoting.shared.web.ApiError;
import br.com.cooperativevoting.shared.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyProperties properties;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(ApiKeyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.hasConfiguredValue()) {
            writeError(response, request, HttpStatus.SERVICE_UNAVAILABLE, "API_KEY_NOT_CONFIGURED",
                "Autenticação da API não está configurada.");
            return;
        }

        String actualApiKey = request.getHeader(properties.headerName());
        if (!matches(actualApiKey, properties.value())) {
            writeError(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Chave de API ausente ou inválida.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(String actualApiKey, String expectedApiKey) {
        if (actualApiKey == null) {
            return false;
        }

        byte[] actual = actualApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedApiKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    private void writeError(
        HttpServletResponse response,
        HttpServletRequest request,
        HttpStatus status,
        String code,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.failure(message, ApiError.of(code), request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
