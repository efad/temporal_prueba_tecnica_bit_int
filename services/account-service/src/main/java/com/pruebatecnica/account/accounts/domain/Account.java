package com.pruebatecnica.account.accounts.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Account(
        String accountId,
        String customerId,
        String accountType,
        String currency,
        BigDecimal initialDeposit,
        String alias,
        String accountNumber,
        AccountStatus status,
        Instant createdAt,
        String idempotencyKey,
        String requestHash
) {
}
