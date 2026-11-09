package com.pruebatecnica.card.cards.infrastructure;

import com.pruebatecnica.card.cards.domain.Card;
import com.pruebatecnica.card.cards.domain.CardRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
class JpaCardRepository implements CardRepository {

    private final CardJpa cards;

    JpaCardRepository(CardJpa cards) {
        this.cards = cards;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findById(String cardId) {
        return cards.findById(cardId).map(CardEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findByIdempotencyKey(String idempotencyKey) {
        return cards.findByIdempotencyKey(idempotencyKey).map(CardEntity::toDomain);
    }

    @Override
    @Transactional
    public Card save(Card card) {
        CardEntity entity = cards.findById(card.cardId()).orElseGet(CardEntity::new);
        entity.copy(card);
        return cards.save(entity).toDomain();
    }
}
