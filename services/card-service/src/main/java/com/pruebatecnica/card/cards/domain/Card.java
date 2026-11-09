package com.pruebatecnica.card.cards.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Card(
        String cardId,
        String accountId,
        String customerId,
        String cardType,
        String maskedPan,
        BigDecimal commissionAmount,
        CommissionStatus commissionStatus,
        CardStatus status,
        String chargeId,
        Instant createdAt,
        String idempotencyKey,
        String requestHash
) {
    public Card charged(String newChargeId) {
        return new Card(
                cardId, accountId, customerId, cardType, maskedPan, commissionAmount,
                CommissionStatus.CHARGED, status, newChargeId, createdAt, idempotencyKey, requestHash);
    }

    public Card refunded() {
        return new Card(
                cardId, accountId, customerId, cardType, maskedPan, commissionAmount,
                CommissionStatus.REFUNDED, status, chargeId, createdAt, idempotencyKey, requestHash);
    }

    public Card reversed() {
        return new Card(
                cardId, accountId, customerId, cardType, maskedPan, commissionAmount,
                commissionStatus, CardStatus.REVERSED, chargeId, createdAt, idempotencyKey, requestHash);
    }
}
