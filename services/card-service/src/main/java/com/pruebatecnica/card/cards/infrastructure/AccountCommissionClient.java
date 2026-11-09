package com.pruebatecnica.card.cards.infrastructure;

import com.pruebatecnica.card.cards.domain.AccountCommissionPort;
import com.pruebatecnica.card.cards.domain.ChargeResult;
import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.RemoteDependencyException;
import com.pruebatecnica.platform.resilience.ResilientCaller;
import com.pruebatecnica.platform.web.CorrelationFilter;
import com.pruebatecnica.platform.web.RequestContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountCommissionClient implements AccountCommissionPort {

    private final RestClient restClient;
    private final ResilientCaller caller;

    public AccountCommissionClient(RestClient accountRestClient, ResilientCaller caller) {
        this.restClient = accountRestClient;
        this.caller = caller;
    }

    @Override
    public ChargeResult charge(String accountId, String cardId, BigDecimal amount, String idempotencyKey) {
        return caller.call("accountService", () -> {
            try {
                ChargePayload payload = restClient.post()
                        .uri("/api/v1/accounts/{accountId}/commission-charges", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .headers(this::forward)
                        .body(new ChargeBody(cardId, amount))
                        .retrieve()
                        .body(ChargePayload.class);
                if (payload == null || payload.chargeId() == null) {
                    throw new RemoteDependencyException("accountService", 502, "account-service no devolvió el cobro");
                }
                return new ChargeResult(payload.chargeId());
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw new RemoteDependencyException("accountService", 503, "Timeout al cobrar la comisión", ex);
            } catch (org.springframework.web.client.RestClientResponseException ex) {
                translate(ex);
                return null;
            }
        });
    }

    @Override
    public void refund(String accountId, String chargeId) {
        caller.call("accountService", () -> {
            try {
                restClient.post()
                        .uri("/api/v1/accounts/{accountId}/commission-charges/{chargeId}/refunds", accountId, chargeId)
                        .headers(this::forward)
                        .retrieve()
                        .toBodilessEntity();
                return Boolean.TRUE;
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw new RemoteDependencyException("accountService", 503, "Timeout al devolver la comisión", ex);
            } catch (org.springframework.web.client.RestClientResponseException ex) {
                translate(ex);
                return Boolean.FALSE;
            }
        });
    }

    private void translate(org.springframework.web.client.RestClientResponseException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            throw new BusinessException("account-service rechazó la comisión: " + ex.getStatusCode().value());
        }
        throw new RemoteDependencyException(
                "accountService",
                ex.getStatusCode().value(),
                "account-service no pudo aplicar la comisión");
    }

    private void forward(org.springframework.http.HttpHeaders headers) {
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

    private record ChargeBody(String cardId, BigDecimal amount) {
    }

    private record ChargePayload(String chargeId, String accountId, String status) {
    }
}
