package com.pruebatecnica.card.cards.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CardJpa extends JpaRepository<CardEntity, String> {

    Optional<CardEntity> findByIdempotencyKey(String idempotencyKey);
}
