package com.pruebatecnica.account.accounts.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AccountJpa extends JpaRepository<AccountEntity, String> {

    Optional<AccountEntity> findByIdempotencyKey(String idempotencyKey);
}
