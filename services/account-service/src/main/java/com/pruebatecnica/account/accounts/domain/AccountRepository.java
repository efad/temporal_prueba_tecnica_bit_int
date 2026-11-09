package com.pruebatecnica.account.accounts.domain;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findById(String accountId);

    Optional<Account> findByIdempotencyKey(String idempotencyKey);

    Account save(Account account);

    Optional<CommissionCharge> findChargeById(String chargeId);

    Optional<CommissionCharge> findChargeByIdempotencyKey(String idempotencyKey);

    CommissionCharge save(CommissionCharge charge);
}
