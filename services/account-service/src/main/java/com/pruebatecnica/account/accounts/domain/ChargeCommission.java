package com.pruebatecnica.account.accounts.domain;

import java.math.BigDecimal;

public record ChargeCommission(
        String idempotencyKey,
        String requestHash,
        String accountId,
        String cardId,
        BigDecimal amount
) {
}
