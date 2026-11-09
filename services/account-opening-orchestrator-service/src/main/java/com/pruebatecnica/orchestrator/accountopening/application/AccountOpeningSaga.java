package com.pruebatecnica.orchestrator.accountopening.application;

import com.pruebatecnica.orchestrator.accountopening.domain.AccountCreated;
import com.pruebatecnica.orchestrator.accountopening.domain.AccountPort;
import com.pruebatecnica.orchestrator.accountopening.domain.CardCreated;
import com.pruebatecnica.orchestrator.accountopening.domain.CardPort;
import com.pruebatecnica.orchestrator.accountopening.domain.OpenAccountCommand;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRepository;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStatus;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStepRecord;
import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.CircuitOpenException;
import com.pruebatecnica.platform.error.ConflictException;
import com.pruebatecnica.platform.error.NotFoundException;
import com.pruebatecnica.platform.error.RemoteDependencyException;
import com.pruebatecnica.platform.web.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.util.Map;

public class AccountOpeningSaga {

    private static final Logger log = LoggerFactory.getLogger(AccountOpeningSaga.class);

    private final AccountPort accounts;
    private final CardPort cards;
    private final SagaRepository repository;
    private final String currency;
    private final BigDecimal initialDeposit;
    private final Map<String, BigDecimal> commissions;

    public AccountOpeningSaga(
            AccountPort accounts,
            CardPort cards,
            SagaRepository repository,
            String currency,
            BigDecimal initialDeposit,
            Map<String, BigDecimal> commissions) {
        this.accounts = accounts;
        this.cards = cards;
        this.repository = repository;
        this.currency = currency;
        this.initialDeposit = initialDeposit;
        this.commissions = Map.copyOf(commissions);
    }

    public SagaRecord open(OpenAccountCommand command) {
        SagaRecord saga = begin(command);
        if (isTerminal(saga.status())) {
            return saga;
        }
        MDC.put("sagaId", saga.sagaId());
        RequestContext.setSagaId(saga.sagaId());
        try {
            saga = createAccount(saga, command);
            if (saga.status() == SagaStatus.PENDING_RETRY) {
                return saga;
            }
            saga = createCard(saga, command);
            if (isTerminal(saga.status())) {
                return saga;
            }
            saga = chargeCommission(saga);
            if (isTerminal(saga.status())) {
                return saga;
            }
            saga = repository.save(saga.withStatus(SagaStatus.COMPLETED, null));
            log.info("sagaId={} status=COMPLETED accountId={} cardId={}", saga.sagaId(), saga.accountId(), saga.cardId());
            return saga;
        } finally {
            MDC.remove("sagaId");
        }
    }

    private boolean isTerminal(SagaStatus status) {
        return status == SagaStatus.COMPLETED
                || status == SagaStatus.FAILED
                || status == SagaStatus.COMPENSATION_FAILED;
    }

    private SagaRecord begin(OpenAccountCommand command) {
        return repository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> resume(existing, command))
                .orElseGet(() -> repository.save(SagaRecord.start(command)));
    }

    private SagaRecord resume(SagaRecord existing, OpenAccountCommand command) {
        if (!existing.requestHash().equals(command.requestHash())) {
            throw new ConflictException("La Idempotency-Key ya se usó con otro cuerpo");
        }
        if (existing.status() != SagaStatus.PENDING_RETRY) {
            return existing;
        }
        return repository.save(existing.restart());
    }

    private SagaRecord createAccount(SagaRecord saga, OpenAccountCommand command) {
        if (saga.status() == SagaStatus.COMPLETED) {
            return saga;
        }
        long started = System.nanoTime();
        try {
            AccountCreated account = accounts.create(
                    saga.sagaId() + ":create-account",
                    command.customerId(),
                    command.productType(),
                    currency,
                    initialDeposit,
                    command.accountAlias());
            long latency = latency(started);
            log.info("sagaId={} step=create-account latencyMs={} result=COMPLETED", saga.sagaId(), latency);
            return repository.save(saga.withAccount(account.accountId()).addStep(SagaStepRecord.action("create-account", latency)));
        } catch (CircuitOpenException | RemoteDependencyException ex) {
            return pendingRetry(saga, "create-account", started, ex);
        }
    }

    private SagaRecord createCard(SagaRecord saga, OpenAccountCommand command) {
        if (saga.status() == SagaStatus.PENDING_RETRY || saga.status() == SagaStatus.COMPLETED) {
            return saga;
        }
        long started = System.nanoTime();
        try {
            CardCreated card = cards.create(
                    saga.sagaId() + ":create-card",
                    saga.accountId(),
                    command.customerId(),
                    command.cardType(),
                    commissionFor(command.cardType()));
            long latency = latency(started);
            log.info("sagaId={} step=create-card latencyMs={} result=COMPLETED", saga.sagaId(), latency);
            return repository.save(saga.withCard(card.cardId()).addStep(SagaStepRecord.action("create-card", latency)));
        } catch (CircuitOpenException | RemoteDependencyException ex) {
            return compensate(recordFailure(saga, "create-card", started, ex), ex.getMessage());
        } catch (BusinessException | ConflictException | NotFoundException ex) {
            return compensate(recordFailure(saga, "create-card", started, ex), ex.getMessage());
        }
    }

    private SagaRecord chargeCommission(SagaRecord saga) {
        if (saga.cardId() == null || (saga.status() != SagaStatus.STARTED && saga.status() != SagaStatus.COMPENSATING)) {
            return saga;
        }
        long started = System.nanoTime();
        try {
            cards.chargeCommission(saga.cardId(), saga.sagaId() + ":charge-commission");
            long latency = latency(started);
            log.info("sagaId={} step=charge-commission latencyMs={} result=COMPLETED", saga.sagaId(), latency);
            return repository.save(saga.addStep(SagaStepRecord.action("charge-commission", latency)));
        } catch (CircuitOpenException | RemoteDependencyException ex) {
            return compensate(recordFailure(saga, "charge-commission", started, ex), ex.getMessage());
        } catch (BusinessException | ConflictException | NotFoundException ex) {
            return compensate(recordFailure(saga, "charge-commission", started, ex), ex.getMessage());
        }
    }

    private SagaRecord pendingRetry(SagaRecord saga, String step, long started, RuntimeException ex) {
        long latency = latency(started);
        log.warn("sagaId={} step={} latencyMs={} result=PENDING_RETRY detail={}", saga.sagaId(), step, latency, ex.getMessage());
        return repository.save(saga
                .addStep(SagaStepRecord.actionFailed(step, latency, ex.getMessage()))
                .withStatus(SagaStatus.PENDING_RETRY, ex.getMessage()));
    }

    private SagaRecord recordFailure(SagaRecord saga, String step, long started, RuntimeException ex) {
        long latency = latency(started);
        log.warn("sagaId={} step={} latencyMs={} result=FAILED detail={}", saga.sagaId(), step, latency, ex.getMessage());
        return repository.save(saga.addStep(SagaStepRecord.actionFailed(step, latency, ex.getMessage())));
    }

    private SagaRecord compensate(SagaRecord saga, String reason) {
        saga = repository.save(saga.withStatus(SagaStatus.COMPENSATING, reason));
        boolean compensationFailed = false;
        if (saga.cardId() != null) {
            String cardId = saga.cardId();
            saga = compensateStep(saga, "reverse-card", () -> cards.reverse(cardId));
            compensationFailed = compensationFailed || lastFailed(saga);
        }
        if (saga.accountId() != null) {
            SagaRecord current = saga;
            saga = compensateStep(saga, "reverse-account", () -> accounts.reverse(current.accountId()));
            compensationFailed = compensationFailed || lastFailed(saga);
        }
        SagaStatus status = compensationFailed ? SagaStatus.COMPENSATION_FAILED : SagaStatus.FAILED;
        saga = repository.save(saga.withStatus(status, reason));
        log.info("sagaId={} status={} compensations={}", saga.sagaId(), status, saga.steps());
        return saga;
    }

    private SagaRecord compensateStep(SagaRecord saga, String name, Runnable action) {
        try {
            action.run();
            log.info("sagaId={} compensation={} result=COMPENSATED", saga.sagaId(), name);
            return repository.save(saga.addStep(SagaStepRecord.compensated(name)));
        } catch (RuntimeException ex) {
            log.error("sagaId={} compensation={} result=COMPENSATION_FAILED detail={}", saga.sagaId(), name, ex.getMessage());
            return repository.save(saga.addStep(SagaStepRecord.compensationFailed(name, ex.getMessage())));
        }
    }

    private boolean lastFailed(SagaRecord saga) {
        if (saga.steps().isEmpty()) {
            return false;
        }
        return "COMPENSATION_FAILED".equals(saga.steps().get(saga.steps().size() - 1).status());
    }

    private BigDecimal commissionFor(String cardType) {
        BigDecimal amount = commissions.get(cardType);
        if (amount == null) {
            throw new BusinessException("No hay comisión configurada para " + cardType);
        }
        return amount;
    }

    private long latency(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
