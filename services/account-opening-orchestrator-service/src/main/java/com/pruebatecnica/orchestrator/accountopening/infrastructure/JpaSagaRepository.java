package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
class JpaSagaRepository implements SagaRepository {

    private final SagaJpa sagas;
    private final ObjectMapper mapper;

    JpaSagaRepository(SagaJpa sagas, ObjectMapper mapper) {
        this.sagas = sagas;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SagaRecord> findById(String sagaId) {
        return sagas.findById(sagaId).map(entity -> entity.toDomain(mapper));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SagaRecord> findByIdempotencyKey(String idempotencyKey) {
        return sagas.findByIdempotencyKey(idempotencyKey).map(entity -> entity.toDomain(mapper));
    }

    @Override
    @Transactional
    public SagaRecord save(SagaRecord record) {
        SagaEntity entity = sagas.findById(record.sagaId()).orElseGet(SagaEntity::new);
        entity.copy(record, mapper);
        return sagas.save(entity).toDomain(mapper);
    }
}
