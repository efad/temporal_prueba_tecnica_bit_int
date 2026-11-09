package com.pruebatecnica.card.cards.domain;

import java.util.Optional;

public interface CardRepository {

    Optional<Card> findById(String cardId);

    Optional<Card> findByIdempotencyKey(String idempotencyKey);

    Card save(Card card);
}
