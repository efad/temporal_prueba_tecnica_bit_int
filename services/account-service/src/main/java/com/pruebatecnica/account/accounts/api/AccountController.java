package com.pruebatecnica.account.accounts.api;

import com.pruebatecnica.account.accounts.application.AccountApplicationService;
import com.pruebatecnica.account.accounts.domain.Account;
import com.pruebatecnica.account.accounts.domain.ChargeCommission;
import com.pruebatecnica.account.accounts.domain.CommissionCharge;
import com.pruebatecnica.account.accounts.domain.CreateAccount;
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
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountApplicationService accounts;

    public AccountController(AccountApplicationService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody AccountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Simulation.apply("ACCOUNT_CREATE");
        Account account = accounts.create(new CreateAccount(
                idempotencyKey,
                hash(request),
                request.customerId(),
                request.accountType(),
                request.currency(),
                request.initialDeposit(),
                request.alias()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/{accountId}/reversals")
    public AccountResponse reverse(@PathVariable String accountId) {
        Simulation.apply("ACCOUNT_REVERSAL");
        return AccountResponse.from(accounts.reverse(accountId));
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable String accountId) {
        return AccountResponse.from(accounts.get(accountId));
    }

    @PostMapping("/{accountId}/commission-charges")
    public ResponseEntity<CommissionChargeResponse> charge(
            @PathVariable String accountId,
            @Valid @RequestBody CommissionChargeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Simulation.apply("ACCOUNT_CHARGE");
        CommissionCharge charge = accounts.charge(new ChargeCommission(
                idempotencyKey,
                RequestHash.of(accountId, request.cardId(), request.amount().setScale(2, RoundingMode.HALF_UP).toPlainString()),
                accountId,
                request.cardId(),
                request.amount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommissionChargeResponse.from(charge));
    }

    @PostMapping("/{accountId}/commission-charges/{chargeId}/refunds")
    public CommissionChargeResponse refund(@PathVariable String accountId, @PathVariable String chargeId) {
        Simulation.apply("ACCOUNT_REFUND");
        return CommissionChargeResponse.from(accounts.refund(accountId, chargeId));
    }

    private String hash(AccountRequest request) {
        String alias = request.alias() == null ? "" : request.alias();
        return RequestHash.of(
                request.customerId(),
                request.accountType(),
                request.currency(),
                request.initialDeposit().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                alias);
    }
}
