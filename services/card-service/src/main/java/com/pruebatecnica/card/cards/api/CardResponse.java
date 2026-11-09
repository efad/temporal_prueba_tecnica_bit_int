package com.pruebatecnica.card.cards.api;

import com.pruebatecnica.card.cards.domain.Card;

public record CardResponse(
        String cardId,
        String maskedPan,
        String commissionStatus,
        String status
) {
    static CardResponse from(Card card) {
        return new CardResponse(card.cardId(), card.maskedPan(), card.commissionStatus().name(), card.status().name());
    }
}
