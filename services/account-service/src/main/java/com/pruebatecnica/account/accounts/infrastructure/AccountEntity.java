package com.pruebatecnica.account.accounts.infrastructure;

import com.pruebatecnica.account.accounts.domain.Account;
import com.pruebatecnica.account.accounts.domain.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
class AccountEntity {

    @Id
    private String accountId;
    private String customerId;
    private String accountType;
    private String currency;
    @Column(precision = 19, scale = 2)
    private BigDecimal initialDeposit;
    private String alias;
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    private Instant createdAt;
    @Column(unique = true)
    private String idempotencyKey;
    private String requestHash;

    static AccountEntity from(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.copy(account);
        return entity;
    }

    void copy(Account account) {
        this.accountId = account.accountId();
        this.customerId = account.customerId();
        this.accountType = account.accountType();
        this.currency = account.currency();
        this.initialDeposit = account.initialDeposit();
        this.alias = account.alias();
        this.accountNumber = account.accountNumber();
        this.status = account.status();
        this.createdAt = account.createdAt();
        this.idempotencyKey = account.idempotencyKey();
        this.requestHash = account.requestHash();
    }

    Account toDomain() {
        return new Account(
                accountId,
                customerId,
                accountType,
                currency,
                initialDeposit,
                alias,
                accountNumber,
                status,
                createdAt,
                idempotencyKey,
                requestHash);
    }
}
