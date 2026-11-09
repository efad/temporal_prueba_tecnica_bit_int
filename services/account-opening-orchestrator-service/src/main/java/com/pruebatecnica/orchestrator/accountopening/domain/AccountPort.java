package com.pruebatecnica.orchestrator.accountopening.domain;

import java.math.BigDecimal;

public interface AccountPort {

    AccountCreated create(
            String idempotencyKey,
            String customerId,
            String accountType,
            String currency,
            BigDecimal initialDeposit,
            String alias);

    void reverse(String accountId);
}
