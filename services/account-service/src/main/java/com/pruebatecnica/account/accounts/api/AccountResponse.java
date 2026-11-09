package com.pruebatecnica.account.accounts.api;

import com.pruebatecnica.account.accounts.domain.Account;

import java.time.Instant;

public record AccountResponse(
        String accountId,
        String accountNumber,
        String status,
        Instant createdAt
) {
    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.accountId(),
                account.accountNumber(),
                account.status().name(),
                account.createdAt());
    }
}
