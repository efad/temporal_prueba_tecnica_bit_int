package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import com.pruebatecnica.orchestrator.accountopening.domain.AccountCreated;
import com.pruebatecnica.orchestrator.accountopening.domain.AccountPort;
import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.RemoteDependencyException;
import com.pruebatecnica.platform.resilience.ResilientCaller;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class AccountClient implements AccountPort {

    private final RestClient restClient;
    private final ResilientCaller caller;

    public AccountClient(RestClient accountRestClient, ResilientCaller caller) {
        this.restClient = accountRestClient;
        this.caller = caller;
    }

    @Override
    public AccountCreated create(
            String idempotencyKey,
            String customerId,
            String accountType,
            String currency,
            BigDecimal initialDeposit,
            String alias) {
        return caller.call("accountService", () -> {
            try {
                AccountPayload payload = restClient.post()
                        .uri("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .headers(Downstream::forward)
                        .body(new AccountBody(customerId, accountType, currency, initialDeposit, alias))
                        .retrieve()
                        .body(AccountPayload.class);
                if (payload == null || payload.accountId() == null) {
                    throw new RemoteDependencyException("accountService", 502, "account-service no devolvió la cuenta");
                }
                return new AccountCreated(payload.accountId(), payload.accountNumber());
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw Downstream.timeout("accountService", ex);
            } catch (RestClientResponseException ex) {
                throw Downstream.translate("accountService", ex);
            }
        });
    }

    @Override
    public void reverse(String accountId) {
        caller.call("accountService", () -> {
            try {
                restClient.post()
                        .uri("/api/v1/accounts/{accountId}/reversals", accountId)
                        .headers(Downstream::forward)
                        .retrieve()
                        .toBodilessEntity();
                return Boolean.TRUE;
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw Downstream.timeout("accountService", ex);
            } catch (RestClientResponseException ex) {
                throw Downstream.translate("accountService", ex);
            }
        });
    }

    private record AccountBody(
            String customerId,
            String accountType,
            String currency,
            BigDecimal initialDeposit,
            String alias
    ) {
    }

    private record AccountPayload(String accountId, String accountNumber, String status, String createdAt) {
    }
}
