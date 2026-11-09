package com.pruebatecnica.card.cards.infrastructure;

import com.pruebatecnica.card.cards.domain.Card;
import com.pruebatecnica.card.cards.domain.CardStatus;
import com.pruebatecnica.card.cards.domain.CommissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cards")
class CardEntity {

    @Id
    private String cardId;
    private String accountId;
    private String customerId;
    private String cardType;
    private String maskedPan;
    @Column(precision = 19, scale = 2)
    private BigDecimal commissionAmount;
    @Enumerated(EnumType.STRING)
    private CommissionStatus commissionStatus;
    @Enumerated(EnumType.STRING)
    private CardStatus status;
    private String chargeId;
    private Instant createdAt;
    @Column(unique = true)
    private String idempotencyKey;
    private String requestHash;

    void copy(Card card) {
        this.cardId = card.cardId();
        this.accountId = card.accountId();
        this.customerId = card.customerId();
        this.cardType = card.cardType();
        this.maskedPan = card.maskedPan();
        this.commissionAmount = card.commissionAmount();
        this.commissionStatus = card.commissionStatus();
        this.status = card.status();
        this.chargeId = card.chargeId();
        this.createdAt = card.createdAt();
        this.idempotencyKey = card.idempotencyKey();
        this.requestHash = card.requestHash();
    }

    Card toDomain() {
        return new Card(
                cardId, accountId, customerId, cardType, maskedPan, commissionAmount,
                commissionStatus, status, chargeId, createdAt, idempotencyKey, requestHash);
    }
}
