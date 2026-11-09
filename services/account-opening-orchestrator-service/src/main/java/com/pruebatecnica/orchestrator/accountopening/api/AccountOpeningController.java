package com.pruebatecnica.orchestrator.accountopening.api;

import com.pruebatecnica.orchestrator.accountopening.application.AccountOpeningService;
import com.pruebatecnica.orchestrator.accountopening.domain.OpenAccountCommand;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStatus;
import com.pruebatecnica.platform.web.RequestContext;
import com.pruebatecnica.platform.web.RequestHash;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/account-openings")
public class AccountOpeningController {

    private final AccountOpeningService openings;

    public AccountOpeningController(AccountOpeningService openings) {
        this.openings = openings;
    }

    @PostMapping
    public ResponseEntity<OpeningResponse> open(
            @Valid @RequestBody OpeningRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        SagaRecord saga = openings.open(new OpenAccountCommand(
                idempotencyKey,
                RequestHash.of(request.customerId(), request.productType(), request.accountAlias(), request.cardType()),
                RequestContext.correlationId(),
                request.customerId(),
                request.productType(),
                request.accountAlias(),
                request.cardType()));
        return ResponseEntity.status(status(saga.status())).body(OpeningResponse.from(saga));
    }

    @GetMapping("/{sagaId}")
    public OpeningDetailsResponse get(@PathVariable String sagaId) {
        return OpeningDetailsResponse.from(openings.get(sagaId), Map.of(
                "accountService", openings.state("accountService"),
                "cardService", openings.state("cardService")));
    }

    private HttpStatus status(SagaStatus sagaStatus) {
        return switch (sagaStatus) {
            case COMPLETED -> HttpStatus.CREATED;
            case PENDING_RETRY -> HttpStatus.SERVICE_UNAVAILABLE;
            case COMPENSATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            case FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case STARTED, COMPENSATING -> HttpStatus.ACCEPTED;
        };
    }
}
