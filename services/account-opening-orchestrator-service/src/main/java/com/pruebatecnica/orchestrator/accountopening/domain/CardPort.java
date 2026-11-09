package com.pruebatecnica.orchestrator.accountopening.domain;

import java.math.BigDecimal;

public interface CardPort {

    CardCreated create(
            String idempotencyKey,
            String accountId,
            String customerId,
            String cardType,
            BigDecimal commissionAmount);

    void chargeCommission(String cardId, String idempotencyKey);

    void reverse(String cardId);
}
