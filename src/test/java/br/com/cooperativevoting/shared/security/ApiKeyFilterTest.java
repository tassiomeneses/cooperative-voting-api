package br.com.cooperativevoting.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiKeyFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldSkipValidationWhenApiKeyIsDisabled() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(new ApiKeyProperties(false, "X-API-Key", "secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/votos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRejectRequestWhenApiKeyIsInvalid() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(new ApiKeyProperties(true, "X-API-Key", "secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/votos");
        request.addHeader("X-API-Key", "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldAcceptRequestWhenApiKeyMatches() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(new ApiKeyProperties(true, "X-API-Key", "secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/votos");
        request.addHeader("X-API-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipHealthEndpoint() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(new ApiKeyProperties(true, "X-API-Key", "secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
