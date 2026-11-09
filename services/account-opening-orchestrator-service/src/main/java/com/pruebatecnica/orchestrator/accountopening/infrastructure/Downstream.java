package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.RemoteDependencyException;
import com.pruebatecnica.platform.web.CorrelationFilter;
import com.pruebatecnica.platform.web.RequestContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

final class Downstream {

    private Downstream() {
    }

    static void forward(HttpHeaders headers) {
        RequestContext.Values values = RequestContext.get();
        if (values == null) {
            return;
        }
        if (values.correlationId() != null) {
            headers.set(CorrelationFilter.CORRELATION_HEADER, values.correlationId());
        }
        if (values.sagaId() != null) {
            headers.set("X-Saga-Id", values.sagaId());
        }
        if (values.delay() != null) {
            headers.set(CorrelationFilter.DELAY_HEADER, values.delay());
        }
        if (values.forceFailure() != null) {
            headers.set(CorrelationFilter.FAILURE_HEADER, values.forceFailure());
        }
    }

    static RuntimeException translate(String dependency, RestClientResponseException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            return new BusinessException(dependency + " rechazó la operación: " + ex.getStatusCode().value());
        }
        return new RemoteDependencyException(dependency, ex.getStatusCode().value(), dependency + " no está disponible");
    }

    static RemoteDependencyException timeout(String dependency, ResourceAccessException ex) {
        return new RemoteDependencyException(dependency, 503, "Timeout al llamar a " + dependency, ex);
    }
}
