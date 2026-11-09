package com.pruebatecnica.orchestrator.accountopening.application;

import com.pruebatecnica.orchestrator.accountopening.domain.AccountPort;
import com.pruebatecnica.orchestrator.accountopening.domain.CardPort;
import com.pruebatecnica.orchestrator.accountopening.domain.OpenAccountCommand;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRecord;
import com.pruebatecnica.orchestrator.accountopening.domain.SagaRepository;
import com.pruebatecnica.orchestrator.accountopening.infrastructure.OpeningProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountOpeningService {

    private static final Logger log = LoggerFactory.getLogger(AccountOpeningService.class);

    private final AccountOpeningSaga saga;
    private final SagaRepository repository;
    private final CircuitBreakerRegistry circuitBreakers;

    public AccountOpeningService(
            AccountPort accounts,
            CardPort cards,
            SagaRepository repository,
            OpeningProperties pricing,
            CircuitBreakerRegistry circuitBreakers) {
        this.saga = new AccountOpeningSaga(
                accounts,
                cards,
                repository,
                pricing.currency(),
                pricing.initialDeposit(),
                pricing.commissions());
        this.repository = repository;
        this.circuitBreakers = circuitBreakers;
    }

    public SagaRecord open(OpenAccountCommand command) {
        SagaRecord result = saga.open(command);
        log.info(
                "sagaId={} status={} accountCircuit={} cardCircuit={}",
                result.sagaId(),
                result.status(),
                state("accountService"),
                state("cardService"));
        return result;
    }

    public SagaRecord get(String sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new com.pruebatecnica.platform.error.NotFoundException("La saga no existe"));
    }

    public String state(String dependency) {
        return circuitBreakers.circuitBreaker(dependency).getState().name();
    }
}
