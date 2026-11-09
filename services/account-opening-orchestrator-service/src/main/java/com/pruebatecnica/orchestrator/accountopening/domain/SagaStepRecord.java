package com.pruebatecnica.orchestrator.accountopening.domain;

public record SagaStepRecord(String name, String kind, String status, long latencyMs, String detail) {

    public static SagaStepRecord action(String name, long latencyMs) {
        return new SagaStepRecord(name, "ACTION", "COMPLETED", latencyMs, null);
    }

    public static SagaStepRecord actionFailed(String name, long latencyMs, String detail) {
        return new SagaStepRecord(name, "ACTION", "FAILED", latencyMs, detail);
    }

    public static SagaStepRecord compensated(String name) {
        return new SagaStepRecord(name, "COMPENSATION", "COMPENSATED", 0, null);
    }

    public static SagaStepRecord compensationFailed(String name, String detail) {
        return new SagaStepRecord(name, "COMPENSATION", "COMPENSATION_FAILED", 0, detail);
    }
}
