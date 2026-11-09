package com.pruebatecnica.account.accounts.domain;

import java.math.BigDecimal;

public record CommissionCharge(
        String chargeId,
        String accountId,
        String cardId,
        BigDecimal amount,
        ChargeStatus status,
        String idempotencyKey,
        String requestHash
) {
}
