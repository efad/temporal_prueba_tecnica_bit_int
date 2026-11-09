package com.pruebatecnica.card.cards.api;

import com.pruebatecnica.card.cards.application.CardApplicationService;
import com.pruebatecnica.card.cards.domain.Card;
import com.pruebatecnica.card.cards.domain.CreateCard;
import com.pruebatecnica.platform.web.RequestHash;
import com.pruebatecnica.platform.web.Simulation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardApplicationService cards;

    public CardController(CardApplicationService cards) {
        this.cards = cards;
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(
            @Valid @RequestBody CardRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Simulation.apply("CARD_CREATE");
        Card card = cards.create(new CreateCard(
                idempotencyKey,
                RequestHash.of(
                        request.accountId(),
                        request.customerId(),
                        request.cardType(),
                        request.commissionAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()),
                request.accountId(),
                request.customerId(),
                request.cardType(),
                request.commissionAmount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(card));
    }

    @PostMapping("/{cardId}/commission-charges")
    public CardResponse charge(
            @PathVariable String cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Simulation.apply("CARD_COMMISSION");
        return CardResponse.from(cards.charge(cardId, idempotencyKey));
    }

    @PostMapping("/{cardId}/reversals")
    public CardResponse reverse(@PathVariable String cardId) {
        Simulation.apply("CARD_REVERSAL");
        return CardResponse.from(cards.reverse(cardId));
    }

    @GetMapping("/{cardId}")
    public CardResponse get(@PathVariable String cardId) {
        return CardResponse.from(cards.get(cardId));
    }
}
