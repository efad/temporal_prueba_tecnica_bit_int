package com.pruebatecnica.account.accounts.application;

import com.pruebatecnica.account.accounts.domain.Account;
import com.pruebatecnica.account.accounts.domain.AccountRepository;
import com.pruebatecnica.account.accounts.domain.Accounts;
import com.pruebatecnica.account.accounts.domain.ChargeCommission;
import com.pruebatecnica.account.accounts.domain.CommissionCharge;
import com.pruebatecnica.account.accounts.domain.CreateAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountApplicationService {

    private final Accounts accounts;

    public AccountApplicationService(AccountRepository repository) {
        this.accounts = new Accounts(repository);
    }

    @Transactional
    public Account create(CreateAccount command) {
        return accounts.create(command);
    }

    @Transactional
    public Account reverse(String accountId) {
        return accounts.reverse(accountId);
    }

    @Transactional(readOnly = true)
    public Account get(String accountId) {
        return accounts.get(accountId);
    }

    @Transactional
    public CommissionCharge charge(ChargeCommission command) {
        return accounts.charge(command);
    }

    @Transactional
    public CommissionCharge refund(String accountId, String chargeId) {
        return accounts.refund(accountId, chargeId);
    }
}
