package com.pruebatecnica.orchestrator.accountopening.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OpeningRequest(
        @NotBlank @Size(max = 64) String customerId,
        @NotBlank @Pattern(regexp = "SAVINGS|CHECKING") String productType,
        @NotBlank @Size(max = 40) String accountAlias,
        @NotBlank @Pattern(regexp = "DEBIT|CREDIT") String cardType
) {
}
