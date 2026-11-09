package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStatus;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaStepRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "sagas")
class SagaEntity {

    @Id
    private String sagaId;
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    private String currentStep;
    private String correlationId;
    @Column(unique = true)
    private String idempotencyKey;
    private String requestHash;
    private String customerId;
    private String productType;
    private String accountAlias;
    private String cardType;
    private String accountId;
    private String cardId;
    @Column(length = 1000)
    private String failureReason;
    @Lob
    private String stepsJson;

    void copy(SagaRecord record, ObjectMapper mapper) {
        this.sagaId = record.sagaId();
        this.status = record.status();
        this.currentStep = record.currentStep();
        this.correlationId = record.correlationId();
        this.idempotencyKey = record.idempotencyKey();
        this.requestHash = record.requestHash();
        this.customerId = record.customerId();
        this.productType = record.productType();
        this.accountAlias = record.accountAlias();
        this.cardType = record.cardType();
        this.accountId = record.accountId();
        this.cardId = record.cardId();
        this.failureReason = record.failureReason();
        try {
            this.stepsJson = mapper.writeValueAsString(record.steps());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo guardar el detalle de la saga", ex);
        }
    }

    SagaRecord toDomain(ObjectMapper mapper) {
        try {
            List<SagaStepRecord> steps = stepsJson == null
                    ? List.of()
                    : mapper.readValue(stepsJson, new TypeReference<List<SagaStepRecord>>() {
                    });
            return new SagaRecord(
                    sagaId, status, currentStep, correlationId, idempotencyKey, requestHash,
                    customerId, productType, accountAlias, cardType, accountId, cardId, failureReason, steps);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer el detalle de la saga", ex);
        }
    }
}
