package com.pruebatecnica.orchestrator.accountopening.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "app.opening")
public record OpeningProperties(String currency, BigDecimal initialDeposit, Map<String, BigDecimal> commissions) {
}
