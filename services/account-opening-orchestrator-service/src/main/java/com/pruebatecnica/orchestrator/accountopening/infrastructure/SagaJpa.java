package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SagaJpa extends JpaRepository<SagaEntity, String> {

    Optional<SagaEntity> findByIdempotencyKey(String idempotencyKey);
}
