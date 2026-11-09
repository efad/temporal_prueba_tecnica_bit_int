package com.pruebatecnica.platform.web;

import com.pruebatecnica.platform.error.BusinessException;
import com.pruebatecnica.platform.error.RemoteDependencyException;

public final class Simulation {

    public static final long MAX_DELAY_MS = 10_000L;

    private Simulation() {
    }

    public static void apply(String operation) {
        RequestContext.Values values = RequestContext.get();
        if (values == null || !values.simulationEnabled()) {
            return;
        }
        sleepIfRequested(values.delay());
        if (operation.equals(values.forceFailure())) {
            throw new RemoteDependencyException(operation, 503, "Fallo simulado de " + operation);
        }
    }

    private static void sleepIfRequested(String delayHeader) {
        if (delayHeader == null || delayHeader.isBlank()) {
            return;
        }
        long delay;
        try {
            delay = Long.parseLong(delayHeader.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("X-Delay-Milliseconds debe ser un número entero");
        }
        if (delay <= 0) {
            return;
        }
        if (delay > MAX_DELAY_MS) {
            delay = MAX_DELAY_MS;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RemoteDependencyException("simulation", 503, "La simulación de latencia fue interrumpida", ex);
        }
    }
}
