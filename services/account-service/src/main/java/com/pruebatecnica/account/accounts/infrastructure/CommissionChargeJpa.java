package com.pruebatecnica.account.accounts.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CommissionChargeJpa extends JpaRepository<CommissionChargeEntity, String> {

    Optional<CommissionChargeEntity> findByIdempotencyKey(String idempotencyKey);
}
