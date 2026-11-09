package com.pruebatecnica.platform.resilience;

import com.pruebatecnica.platform.error.CircuitOpenException;
import com.pruebatecnica.platform.error.RemoteDependencyException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.util.function.Supplier;

public class ResilientCaller {

    private final CircuitBreakerRegistry circuitBreakers;
    private final BulkheadRegistry bulkheads;

    public ResilientCaller(CircuitBreakerRegistry circuitBreakers, BulkheadRegistry bulkheads) {
        this.circuitBreakers = circuitBreakers;
        this.bulkheads = bulkheads;
    }

    public <T> T call(String dependency, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakers.circuitBreaker(dependency);
        Bulkhead bulkhead = bulkheads.bulkhead(dependency);
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Bulkhead.decorateSupplier(bulkhead, supplier));
        try {
            return decorated.get();
        } catch (CallNotPermittedException ex) {
            throw new CircuitOpenException(dependency, ex);
        } catch (BulkheadFullException ex) {
            throw new RemoteDependencyException(dependency, 503, "Capacidad agotada hacia " + dependency, ex);
        }
    }
}
