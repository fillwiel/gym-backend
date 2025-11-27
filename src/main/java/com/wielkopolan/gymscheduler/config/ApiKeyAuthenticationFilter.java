package com.wielkopolan.gymscheduler.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * A custom filter for API key authentication. This filter is executed once per request
 * and handles authentication for both actuator endpoints and API requests.
 */
@Slf4j
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    /**
     * The valid API key used to authenticate API requests.
     * This value is injected from the application properties.
     */
    private final String validApiKey;

    /**
     * The port on which the management server (e.g., actuator endpoints) is running.
     * Defaults to 5000 if not specified in the application properties.
     */
    private final int managementPort;

    public ApiKeyAuthenticationFilter(@Value("${app.api.key}") String validApiKey,
                                      @Value("${management.server.port:5000}") int managementPort) {
        this.validApiKey = validApiKey;
        this.managementPort = managementPort;
    }

    /**
     * Filters incoming requests to authenticate based on the API key or allow access
     * to actuator endpoints without requiring an API key.
     *
     * @param request     The HTTP request.
     * @param response    The HTTP response.
     * @param filterChain The filter chain to pass the request/response to the next filter.
     * @throws ServletException If an error occurs during filtering.
     * @throws IOException      If an I/O error occurs during filtering.
     */
    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        // Check if the request is for the management port (actuator endpoints)
        if (request.getLocalPort() == managementPort) {
            log.debug("Actuator endpoint accessed on port: {}", managementPort);
            setAuthentication("actuator-user", "ROLE_ACTUATOR");
        } else {
            // Handle API key authentication for other requests
            final var apiKey = request.getHeader("X-API-Key");
            if (apiKey == null || !isValidApiKey(apiKey)) {
                log.warn("Invalid API key from: {}", request.getRemoteAddr());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("Missing API key");
                return;
            }
            setAuthentication("api-user", "ROLE_USER");
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to set authentication in the security context.
     */
    private void setAuthentication(final String username, final String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority(role)))
        );
    }

    /**
     * Helper method to validate the API key.
     */
    private boolean isValidApiKey(final String apiKey) {
        return MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), validApiKey.getBytes(StandardCharsets.UTF_8));
    }
}