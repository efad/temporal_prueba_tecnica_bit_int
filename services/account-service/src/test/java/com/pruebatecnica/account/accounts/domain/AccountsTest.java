package com.pruebatecnica.account.accounts.domain;

import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountsTest {

    @Test
    @DisplayName("CA-BASE-04 repetir el reverso de la cuenta no cambia el resultado")
    void reverseIsIdempotent() {
        InMemoryAccounts repository = new InMemoryAccounts();
        Accounts accounts = new Accounts(repository);
        Account created = accounts.create(command("key-1", "hash-1"));

        Account first = accounts.reverse(created.accountId());
        Account second = accounts.reverse(created.accountId());

        assertEquals(AccountStatus.REVERSED, first.status());
        assertEquals(AccountStatus.REVERSED, second.status());
        assertEquals(first.accountNumber(), second.accountNumber());
    }

    @Test
    @DisplayName("RF-BASE-04 la misma Idempotency-Key y el mismo cuerpo devuelven la cuenta guardada")
    void sameIdempotencyKeyReturnsStoredAccount() {
        Accounts accounts = new Accounts(new InMemoryAccounts());
        Account first = accounts.create(command("key-1", "hash-1"));
        Account second = accounts.create(command("key-1", "hash-1"));

        assertEquals(first.accountId(), second.accountId());
    }

    @Test
    @DisplayName("CA-BASE-06 otra petición con la misma Idempotency-Key responde conflicto")
    void differentBodyWithSameKeyConflicts() {
        Accounts accounts = new Accounts(new InMemoryAccounts());
        accounts.create(command("key-1", "hash-1"));

        assertThrows(ConflictException.class, () -> accounts.create(command("key-1", "hash-2")));
    }

    @Test
    @DisplayName("CA-03 una cuenta revertida rechaza un cobro de comisión")
    void reversedAccountRejectsCharge() {
        Accounts accounts = new Accounts(new InMemoryAccounts());
        Account created = accounts.create(command("key-1", "hash-1"));
        accounts.reverse(created.accountId());

        assertThrows(BusinessException.class, () -> accounts.charge(new ChargeCommission(
                "charge-1", "charge-hash", created.accountId(), "card-1", new BigDecimal("15000.00"))));
    }

    private CreateAccount command(String key, String hash) {
        return new CreateAccount(key, hash, "customer-1", "SAVINGS", "COP", new BigDecimal("0.00"), "nomina");
    }

    private static final class InMemoryAccounts implements AccountRepository {
        private final Map<String, Account> accounts = new HashMap<>();
        private final Map<String, CommissionCharge> charges = new HashMap<>();

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(accounts.get(accountId));
        }

        @Override
        public Optional<Account> findByIdempotencyKey(String idempotencyKey) {
            return accounts.values().stream().filter(account -> idempotencyKey.equals(account.idempotencyKey())).findFirst();
        }

        @Override
        public Account save(Account account) {
            accounts.put(account.accountId(), account);
            return account;
        }

        @Override
        public Optional<CommissionCharge> findChargeById(String chargeId) {
            return Optional.ofNullable(charges.get(chargeId));
        }

        @Override
        public Optional<CommissionCharge> findChargeByIdempotencyKey(String idempotencyKey) {
            return charges.values().stream().filter(charge -> idempotencyKey.equals(charge.idempotencyKey())).findFirst();
        }

        @Override
        public CommissionCharge save(CommissionCharge charge) {
            charges.put(charge.chargeId(), charge);
            return charge;
        }
    }
}
