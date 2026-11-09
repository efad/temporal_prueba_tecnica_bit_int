package com.pruebatecnica.account.accounts.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CommissionChargeRequest(
        @NotBlank String cardId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
