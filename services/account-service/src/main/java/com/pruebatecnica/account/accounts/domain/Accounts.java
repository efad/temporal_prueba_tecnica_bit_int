package com.pruebatecnica.account.accounts.domain;

import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.ConflictException;
import com.pruebatecnica.platform.error.NotFoundException;

import java.time.Instant;
import java.util.SplittableRandom;
import java.util.UUID;

public class Accounts {

    private final AccountRepository repository;
    private final SplittableRandom random = new SplittableRandom();

    public Accounts(AccountRepository repository) {
        this.repository = repository;
    }

    public Account create(CreateAccount command) {
        return repository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> sameRequest(existing.requestHash(), command.requestHash(), existing))
                .orElseGet(() -> repository.save(new Account(
                        UUID.randomUUID().toString(),
                        command.customerId(),
                        command.accountType(),
                        command.currency(),
                        command.initialDeposit(),
                        command.alias(),
                        accountNumber(),
                        AccountStatus.CREATED,
                        Instant.now(),
                        command.idempotencyKey(),
                        command.requestHash())));
    }

    public Account get(String accountId) {
        return repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("La cuenta no existe"));
    }

    public Account reverse(String accountId) {
        Account account = repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("La cuenta no existe"));
        if (account.status() == AccountStatus.REVERSED) {
            return account;
        }
        return repository.save(new Account(
                account.accountId(),
                account.customerId(),
                account.accountType(),
                account.currency(),
                account.initialDeposit(),
                account.alias(),
                account.accountNumber(),
                AccountStatus.REVERSED,
                account.createdAt(),
                account.idempotencyKey(),
                account.requestHash()));
    }

    public CommissionCharge charge(ChargeCommission command) {
        return repository.findChargeByIdempotencyKey(command.idempotencyKey())
                .map(existing -> sameRequest(existing.requestHash(), command.requestHash(), existing))
                .orElseGet(() -> {
                    Account account = repository.findById(command.accountId())
                            .orElseThrow(() -> new NotFoundException("La cuenta no existe"));
                    if (account.status() != AccountStatus.CREATED) {
                        throw new BusinessException("La cuenta no admite el cobro de la comisión");
                    }
                    if (command.amount().signum() <= 0) {
                        throw new BusinessException("La comisión debe ser mayor que cero");
                    }
                    return repository.save(new CommissionCharge(
                            UUID.randomUUID().toString(),
                            command.accountId(),
                            command.cardId(),
                            command.amount(),
                            ChargeStatus.CHARGED,
                            command.idempotencyKey(),
                            command.requestHash()));
                });
    }

    public CommissionCharge refund(String accountId, String chargeId) {
        CommissionCharge charge = repository.findChargeById(chargeId)
                .orElseThrow(() -> new NotFoundException("El cobro de comisión no existe"));
        if (!charge.accountId().equals(accountId)) {
            throw new NotFoundException("El cobro no pertenece a la cuenta");
        }
        if (charge.status() == ChargeStatus.REFUNDED) {
            return charge;
        }
        return repository.save(new CommissionCharge(
                charge.chargeId(),
                charge.accountId(),
                charge.cardId(),
                charge.amount(),
                ChargeStatus.REFUNDED,
                charge.idempotencyKey(),
                charge.requestHash()));
    }

    private String accountNumber() {
        StringBuilder number = new StringBuilder(11);
        for (int index = 0; index < 11; index++) {
            number.append(random.nextInt(10));
        }
        return number.toString();
    }

    private <T> T sameRequest(String storedHash, String requestHash, T existing) {
        if (!storedHash.equals(requestHash)) {
            throw new ConflictException("La Idempotency-Key ya se usó con otro cuerpo");
        }
        return existing;
    }
}
