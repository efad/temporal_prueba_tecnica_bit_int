package com.pruebatecnica.card.cards.domain;

import java.math.BigDecimal;

public record CreateCard(
        String idempotencyKey,
        String requestHash,
        String accountId,
        String customerId,
        String cardType,
        BigDecimal commissionAmount
) {
}
