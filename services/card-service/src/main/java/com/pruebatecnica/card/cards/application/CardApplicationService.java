package com.pruebatecnica.card.cards.application;

import com.pruebatecnica.card.cards.domain.AccountCommissionPort;
import com.pruebatecnica.card.cards.domain.Card;
import com.pruebatecnica.card.cards.domain.CardRepository;
import com.pruebatecnica.card.cards.domain.Cards;
import com.pruebatecnica.card.cards.domain.CreateCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardApplicationService {

    private final Cards cards;

    public CardApplicationService(CardRepository repository, AccountCommissionPort accounts) {
        this.cards = new Cards(repository, accounts);
    }

    @Transactional
    public Card create(CreateCard command) {
        return cards.create(command);
    }

    public Card charge(String cardId, String idempotencyKey) {
        return cards.charge(cardId, idempotencyKey);
    }

    public Card reverse(String cardId) {
        return cards.reverse(cardId);
    }

    @Transactional(readOnly = true)
    public Card get(String cardId) {
        return cards.get(cardId);
    }
}
