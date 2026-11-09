package com.pruebatecnica.orchestrator.accountopening.api;

import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStepRecord;

import java.util.List;
import java.util.Map;

public record OpeningResponse(
        String sagaId,
        String accountId,
        String cardId,
        String sagaStatus,
        String failureReason
) {
    static OpeningResponse from(SagaRecord saga) {
        return new OpeningResponse(
                saga.sagaId(),
                saga.accountId(),
                saga.cardId(),
                saga.status().name(),
                saga.failureReason());
    }
}

record OpeningDetailsResponse(
        String sagaId,
        String accountId,
        String cardId,
        String sagaStatus,
        String failureReason,
        List<SagaStepRecord> steps,
        Map<String, String> circuitStates
) {
    static OpeningDetailsResponse from(SagaRecord saga, Map<String, String> circuitStates) {
        return new OpeningDetailsResponse(
                saga.sagaId(),
                saga.accountId(),
                saga.cardId(),
                saga.status().name(),
                saga.failureReason(),
                saga.steps(),
                circuitStates);
    }
}
