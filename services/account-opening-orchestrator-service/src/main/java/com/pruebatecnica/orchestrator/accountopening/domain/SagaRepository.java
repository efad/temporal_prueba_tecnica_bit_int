package com.pruebatecnica.orchestrator.accountopening.domain;

import java.util.Optional;

public interface SagaRepository {

    Optional<SagaRecord> findById(String sagaId);

    Optional<SagaRecord> findByIdempotencyKey(String idempotencyKey);

    SagaRecord save(SagaRecord record);
}
