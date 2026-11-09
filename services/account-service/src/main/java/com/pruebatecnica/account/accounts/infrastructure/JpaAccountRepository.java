package com.pruebatecnica.account.accounts.infrastructure;

import com.pruebatecnica.account.accounts.domain.Account;
import com.pruebatecnica.account.accounts.domain.AccountRepository;
import com.pruebatecnica.account.accounts.domain.CommissionCharge;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
class JpaAccountRepository implements AccountRepository {

    private final AccountJpa accounts;
    private final CommissionChargeJpa charges;

    JpaAccountRepository(AccountJpa accounts, CommissionChargeJpa charges) {
        this.accounts = accounts;
        this.charges = charges;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(String accountId) {
        return accounts.findById(accountId).map(AccountEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findByIdempotencyKey(String idempotencyKey) {
        return accounts.findByIdempotencyKey(idempotencyKey).map(AccountEntity::toDomain);
    }

    @Override
    @Transactional
    public Account save(Account account) {
        AccountEntity entity = accounts.findById(account.accountId()).orElseGet(AccountEntity::new);
        entity.copy(account);
        return accounts.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommissionCharge> findChargeById(String chargeId) {
        return charges.findById(chargeId).map(CommissionChargeEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommissionCharge> findChargeByIdempotencyKey(String idempotencyKey) {
        return charges.findByIdempotencyKey(idempotencyKey).map(CommissionChargeEntity::toDomain);
    }

    @Override
    @Transactional
    public CommissionCharge save(CommissionCharge charge) {
        CommissionChargeEntity entity = charges.findById(charge.chargeId()).orElseGet(CommissionChargeEntity::new);
        entity.copy(charge);
        return charges.save(entity).toDomain();
    }
}
