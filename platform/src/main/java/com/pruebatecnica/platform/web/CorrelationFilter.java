package com.pruebatecnica.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CorrelationFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String DELAY_HEADER = "X-Delay-Milliseconds";
    public static final String FAILURE_HEADER = "X-Force-Failure";

    private final boolean simulationEnabled;

    public CorrelationFilter(boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String delay = simulationEnabled ? request.getHeader(DELAY_HEADER) : null;
        String forceFailure = simulationEnabled ? request.getHeader(FAILURE_HEADER) : null;
        RequestContext.set(new RequestContext.Values(correlationId, null, delay, forceFailure, simulationEnabled));
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("sagaId");
            RequestContext.clear();
        }
    }
}
