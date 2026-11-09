package com.pruebatecnica.account.accounts.domain;

import java.math.BigDecimal;

public record CreateAccount(
        String idempotencyKey,
        String requestHash,
        String customerId,
        String accountType,
        String currency,
        BigDecimal initialDeposit,
        String alias
) {
}
