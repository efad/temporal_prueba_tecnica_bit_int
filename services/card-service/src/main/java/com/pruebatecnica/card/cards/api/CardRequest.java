package com.pruebatecnica.card.cards.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CardRequest(
        @NotBlank String accountId,
        @NotBlank String customerId,
        @NotBlank String cardType,
        @NotNull @DecimalMin("0.01") BigDecimal commissionAmount
) {
}
