package com.pruebatecnica.orchestrator.accountopening.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SagaRecord(
        String sagaId,
        SagaStatus status,
        String currentStep,
        String correlationId,
        String idempotencyKey,
        String requestHash,
        String customerId,
        String productType,
        String accountAlias,
        String cardType,
        String accountId,
        String cardId,
        String failureReason,
        List<SagaStepRecord> steps
) {
    public SagaRecord {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public static SagaRecord start(OpenAccountCommand command) {
        return new SagaRecord(
                UUID.randomUUID().toString(),
                SagaStatus.STARTED,
                null,
                command.correlationId(),
                command.idempotencyKey(),
                command.requestHash(),
                command.customerId(),
                command.productType(),
                command.accountAlias(),
                command.cardType(),
                null,
                null,
                null,
                List.of());
    }

    public SagaRecord restart() {
        return copy(SagaStatus.STARTED, null, null, null, null, List.of());
    }

    public SagaRecord withAccount(String newAccountId) {
        return copy(status, "create-account", newAccountId, cardId, failureReason, steps);
    }

    public SagaRecord withCard(String newCardId) {
        return copy(status, "create-card", accountId, newCardId, failureReason, steps);
    }

    public SagaRecord addStep(SagaStepRecord step) {
        List<SagaStepRecord> copy = new ArrayList<>(steps);
        copy.add(step);
        return copy(status, step.name(), accountId, cardId, failureReason, copy);
    }

    public SagaRecord withStatus(SagaStatus newStatus, String reason) {
        return copy(newStatus, currentStep, accountId, cardId, reason, steps);
    }

    private SagaRecord copy(
            SagaStatus newStatus,
            String step,
            String newAccountId,
            String newCardId,
            String reason,
            List<SagaStepRecord> newSteps) {
        return new SagaRecord(
                sagaId,
                newStatus,
                step,
                correlationId,
                idempotencyKey,
                requestHash,
                customerId,
                productType,
                accountAlias,
                cardType,
                newAccountId,
                newCardId,
                reason,
                newSteps);
    }
}
