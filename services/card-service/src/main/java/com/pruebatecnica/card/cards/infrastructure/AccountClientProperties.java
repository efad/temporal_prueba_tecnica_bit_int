package com.pruebatecnica.card.cards.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record AccountClientProperties(String accountServiceUrl) {
}
