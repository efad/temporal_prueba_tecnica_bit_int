package com.pruebatecnica.card.cards.domain;

import java.math.BigDecimal;

public interface AccountCommissionPort {

    ChargeResult charge(String accountId, String cardId, BigDecimal amount, String idempotencyKey);

    void refund(String accountId, String chargeId);
}
