package com.pruebatecnica.account.accounts.api;

import com.pruebatecnica.account.accounts.domain.CommissionCharge;

public record CommissionChargeResponse(
        String chargeId,
        String accountId,
        String status
) {
    static CommissionChargeResponse from(CommissionCharge charge) {
        return new CommissionChargeResponse(charge.chargeId(), charge.accountId(), charge.status().name());
    }
}
