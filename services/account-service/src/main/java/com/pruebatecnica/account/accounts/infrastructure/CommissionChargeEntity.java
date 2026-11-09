package com.pruebatecnica.account.accounts.infrastructure;

import com.pruebatecnica.account.accounts.domain.ChargeStatus;
import com.pruebatecnica.account.accounts.domain.CommissionCharge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_charges")
class CommissionChargeEntity {

    @Id
    private String chargeId;
    private String accountId;
    private String cardId;
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private ChargeStatus status;
    @Column(unique = true)
    private String idempotencyKey;
    private String requestHash;

    static CommissionChargeEntity from(CommissionCharge charge) {
        CommissionChargeEntity entity = new CommissionChargeEntity();
        entity.copy(charge);
        return entity;
    }

    void copy(CommissionCharge charge) {
        this.chargeId = charge.chargeId();
        this.accountId = charge.accountId();
        this.cardId = charge.cardId();
        this.amount = charge.amount();
        this.status = charge.status();
        this.idempotencyKey = charge.idempotencyKey();
        this.requestHash = charge.requestHash();
    }

    CommissionCharge toDomain() {
        return new CommissionCharge(chargeId, accountId, cardId, amount, status, idempotencyKey, requestHash);
    }
}
