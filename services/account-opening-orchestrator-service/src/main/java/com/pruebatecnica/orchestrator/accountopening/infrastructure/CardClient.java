package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import com.pruebatecnica.orchestrator.accountopening.domain.CardCreated;
import com.pruebatecnica.orchestrator.accountopening.domain.CardPort;
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
public class CardClient implements CardPort {

    private final RestClient restClient;
    private final ResilientCaller caller;

    public CardClient(RestClient cardRestClient, ResilientCaller caller) {
        this.restClient = cardRestClient;
        this.caller = caller;
    }

    @Override
    public CardCreated create(
            String idempotencyKey,
            String accountId,
            String customerId,
            String cardType,
            BigDecimal commissionAmount) {
        return caller.call("cardService", () -> {
            try {
                CardPayload payload = restClient.post()
                        .uri("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .headers(Downstream::forward)
                        .body(new CardBody(accountId, customerId, cardType, commissionAmount))
                        .retrieve()
                        .body(CardPayload.class);
                if (payload == null || payload.cardId() == null) {
                    throw new RemoteDependencyException("cardService", 502, "card-service no devolvió la tarjeta");
                }
                return new CardCreated(payload.cardId(), payload.maskedPan());
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw Downstream.timeout("cardService", ex);
            } catch (RestClientResponseException ex) {
                throw Downstream.translate("cardService", ex);
            }
        });
    }

    @Override
    public void chargeCommission(String cardId, String idempotencyKey) {
        caller.call("cardService", () -> {
            try {
                restClient.post()
                        .uri("/api/v1/cards/{cardId}/commission-charges", cardId)
                        .header("Idempotency-Key", idempotencyKey)
                        .headers(Downstream::forward)
                        .retrieve()
                        .toBodilessEntity();
                return Boolean.TRUE;
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw Downstream.timeout("cardService", ex);
            } catch (RestClientResponseException ex) {
                throw Downstream.translate("cardService", ex);
            }
        });
    }

    @Override
    public void reverse(String cardId) {
        caller.call("cardService", () -> {
            try {
                restClient.post()
                        .uri("/api/v1/cards/{cardId}/reversals", cardId)
                        .headers(Downstream::forward)
                        .retrieve()
                        .toBodilessEntity();
                return Boolean.TRUE;
            } catch (BusinessException | RemoteDependencyException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                throw Downstream.timeout("cardService", ex);
            } catch (RestClientResponseException ex) {
                throw Downstream.translate("cardService", ex);
            }
        });
    }

    private record CardBody(String accountId, String customerId, String cardType, BigDecimal commissionAmount) {
    }

    private record CardPayload(String cardId, String maskedPan, String commissionStatus, String status) {
    }
}
