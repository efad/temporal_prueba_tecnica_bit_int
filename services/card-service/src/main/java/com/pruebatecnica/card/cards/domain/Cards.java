package com.pruebatecnica.card.cards.domain;

import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.ConflictException;
import com.pruebatecnica.platform.error.NotFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.UUID;

public class Cards {

    private final CardRepository repository;
    private final AccountCommissionPort accounts;
    private final SplittableRandom random = new SplittableRandom();

    public Cards(CardRepository repository, AccountCommissionPort accounts) {
        this.repository = repository;
        this.accounts = accounts;
    }

    public Card create(CreateCard command) {
        return repository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> {
                    if (!existing.requestHash().equals(command.requestHash())) {
                        throw new ConflictException("La Idempotency-Key ya se usó con otro cuerpo");
                    }
                    return existing;
                })
                .orElseGet(() -> repository.save(new Card(
                        UUID.randomUUID().toString(),
                        command.accountId(),
                        command.customerId(),
                        command.cardType(),
                        maskedPan(),
                        command.commissionAmount(),
                        CommissionStatus.PENDING,
                        CardStatus.CREATED,
                        null,
                        Instant.now(),
                        command.idempotencyKey(),
                        command.requestHash())));
    }

    public Card charge(String cardId, String idempotencyKey) {
        Card card = required(cardId);
        if (card.status() != CardStatus.CREATED) {
            throw new BusinessException("La tarjeta revertida no admite el cobro de la comisión");
        }
        if (card.commissionStatus() == CommissionStatus.CHARGED) {
            return card;
        }
        ChargeResult charge = accounts.charge(card.accountId(), card.cardId(), card.commissionAmount(), idempotencyKey);
        return repository.save(card.charged(charge.chargeId()));
    }

    public Card reverse(String cardId) {
        Card card = required(cardId);
        if (card.status() == CardStatus.REVERSED) {
            return card;
        }
        if (card.commissionStatus() == CommissionStatus.CHARGED && card.chargeId() != null) {
            accounts.refund(card.accountId(), card.chargeId());
            card = repository.save(card.refunded());
        }
        return repository.save(card.reversed());
    }

    public Card get(String cardId) {
        return required(cardId);
    }

    private Card required(String cardId) {
        return repository.findById(cardId).orElseThrow(() -> new NotFoundException("La tarjeta no existe"));
    }

    private String maskedPan() {
        char[] digits = new char[16];
        for (int index = 0; index < digits.length; index++) {
            digits[index] = (char) ('0' + random.nextInt(10));
        }
        String last4 = new String(digits, 12, 4);
        Arrays.fill(digits, '\0');
        return "************" + last4;
    }
}
