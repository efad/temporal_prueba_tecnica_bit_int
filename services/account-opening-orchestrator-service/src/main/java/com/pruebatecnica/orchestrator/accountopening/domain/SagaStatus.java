package com.pruebatecnica.orchestrator.accountopening.domain;

public enum SagaStatus {
    STARTED,
    COMPLETED,
    COMPENSATING,
    FAILED,
    COMPENSATION_FAILED,
    PENDING_RETRY
}
