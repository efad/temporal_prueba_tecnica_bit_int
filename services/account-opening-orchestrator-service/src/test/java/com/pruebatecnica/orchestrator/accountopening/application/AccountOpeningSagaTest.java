package com.pruebatecnica.orchestrator.accountopening.application;

import com.pruebatecnica.orchestrator.accountopening.domain.AccountCreated;
import com.pruebatecnica.orchestrator.accountopening.domain.AccountPort;
import com.pruebatecnica.orchestrator.accountopening.domain.CardCreated;
import com.pruebatecnica.orchestrator.accountopening.domain.CardPort;
import com.pruebatecnica.orchestrator.accountopening.domain.OpenAccountCommand;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRepository;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStatus;
import com.pruebatecnica.platform.error.CircuitOpenException;
import com.pruebatecnica.platform.error.ConflictException;
import com.pruebatecnica.platform.error.RemoteDependencyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountOpeningSagaTest {

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeCards cards = new FakeCards();
    private final InMemorySagas sagas = new InMemorySagas();
    private final AccountOpeningSaga saga = new AccountOpeningSaga(
            accounts, cards, sagas, "COP", new BigDecimal("0.00"),
            Map.of("DEBIT", new BigDecimal("15000.00"), "CREDIT", new BigDecimal("35000.00")));

    @Test
    @DisplayName("CA-01 el orquestador completa cuenta, tarjeta y comisión")
    void successCompletesTheThreeSteps() {
        SagaRecord result = saga.open(command("key-1", "hash-1"));

        assertEquals(SagaStatus.COMPLETED, result.status());
        assertEquals(List.of("create-account"), accounts.calls);
        assertEquals(List.of("create-card", "charge-commission"), cards.calls);
        assertEquals("account-1", result.accountId());
        assertEquals("card-1", result.cardId());
    }

    @Test
    @DisplayName("CA-03 si falla la tarjeta se revierte la cuenta")
    void cardFailureCompensatesTheAccount() {
        cards.failCreate = true;

        SagaRecord result = saga.open(command("key-2", "hash-2"));

        assertEquals(SagaStatus.FAILED, result.status());
        assertEquals(List.of("create-account", "reverse-account"), accounts.calls);
        assertEquals(List.of("create-card"), cards.calls);
    }

    @Test
    @DisplayName("CA-03 si falla la comisión se revierten tarjeta y cuenta")
    void commissionFailureCompensatesCardAndAccount() {
        cards.failCharge = true;

        SagaRecord result = saga.open(command("key-3", "hash-3"));

        assertEquals(SagaStatus.FAILED, result.status());
        assertEquals(List.of("create-card", "charge-commission", "reverse-card"), cards.calls);
        assertEquals(List.of("create-account", "reverse-account"), accounts.calls);
    }

    @Test
    @DisplayName("CA-BASE-05 una compensación fallida deja COMPENSATION_FAILED")
    void compensationFailureIsVisible() {
        cards.failCharge = true;
        cards.failReverse = true;

        SagaRecord result = saga.open(command("key-4", "hash-4"));

        assertEquals(SagaStatus.COMPENSATION_FAILED, result.status());
        assertEquals(List.of("create-account", "reverse-account"), accounts.calls);
    }

    @Test
    @DisplayName("CA-04 un circuito abierto antes de crear la cuenta deja PENDING_RETRY")
    void openCircuitBeforeAnyStepCanBeRetried() {
        accounts.failCreateWithOpenCircuit = true;
        SagaRecord pending = saga.open(command("key-5", "hash-5"));
        assertEquals(SagaStatus.PENDING_RETRY, pending.status());
        assertNull(pending.accountId());

        accounts.failCreateWithOpenCircuit = false;
        SagaRecord completed = saga.open(command("key-5", "hash-5"));

        assertEquals(pending.sagaId(), completed.sagaId());
        assertEquals(SagaStatus.COMPLETED, completed.status());
    }

    @Test
    @DisplayName("RF-BASE-04 la misma Idempotency-Key con otro cuerpo entra en conflicto")
    void idempotencyConflict() {
        saga.open(command("key-6", "hash-6"));

        assertThrows(ConflictException.class, () -> saga.open(command("key-6", "hash-7")));
    }

    private OpenAccountCommand command(String key, String hash) {
        return new OpenAccountCommand(key, hash, "correlation-1", "customer-1", "SAVINGS", "nomina", "DEBIT");
    }

    private static final class FakeAccounts implements AccountPort {
        private final List<String> calls = new ArrayList<>();
        private boolean failCreateWithOpenCircuit;

        @Override
        public AccountCreated create(String idempotencyKey, String customerId, String accountType, String currency, BigDecimal initialDeposit, String alias) {
            calls.add("create-account");
            if (failCreateWithOpenCircuit) {
                throw new CircuitOpenException("accountService", new IllegalStateException("open"));
            }
            return new AccountCreated("account-1", "00000000001");
        }

        @Override
        public void reverse(String accountId) {
            calls.add("reverse-account");
        }
    }

    private static final class FakeCards implements CardPort {
        private final List<String> calls = new ArrayList<>();
        private boolean failCreate;
        private boolean failCharge;
        private boolean failReverse;

        @Override
        public CardCreated create(String idempotencyKey, String accountId, String customerId, String cardType, BigDecimal commissionAmount) {
            calls.add("create-card");
            if (failCreate) {
                throw new RemoteDependencyException("cardService", 503, "Fallo simulado de tarjeta");
            }
            return new CardCreated("card-1", "************1234");
        }

        @Override
        public void chargeCommission(String cardId, String idempotencyKey) {
            calls.add("charge-commission");
            if (failCharge) {
                throw new RemoteDependencyException("cardService", 503, "Fallo simulado de comisión");
            }
        }

        @Override
        public void reverse(String cardId) {
            calls.add("reverse-card");
            if (failReverse) {
                throw new RemoteDependencyException("cardService", 503, "No se pudo revertir la tarjeta");
            }
        }
    }

    private static final class InMemorySagas implements SagaRepository {
        private final Map<String, SagaRecord> byId = new HashMap<>();

        @Override
        public Optional<SagaRecord> findById(String sagaId) {
            return Optional.ofNullable(byId.get(sagaId));
        }

        @Override
        public Optional<SagaRecord> findByIdempotencyKey(String idempotencyKey) {
            return byId.values().stream().filter(saga -> idempotencyKey.equals(saga.idempotencyKey())).findFirst();
        }

        @Override
        public SagaRecord save(SagaRecord record) {
            byId.put(record.sagaId(), record);
            return record;
        }
    }
}
