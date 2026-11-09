package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record ClientProperties(String accountServiceUrl, String cardServiceUrl) {
}
