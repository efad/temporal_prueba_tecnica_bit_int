package com.pruebatecnica.orchestrator.accountopening.domain;

public record OpenAccountCommand(
        String idempotencyKey,
        String requestHash,
        String correlationId,
        String customerId,
        String productType,
        String accountAlias,
        String cardType
) {
}
