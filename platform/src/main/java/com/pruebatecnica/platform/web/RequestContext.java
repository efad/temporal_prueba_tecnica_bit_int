package com.pruebatecnica.platform.web;

public final class RequestContext {

    public record Values(
            String correlationId,
            String sagaId,
            String delay,
            String forceFailure,
            boolean simulationEnabled
    ) {
    }

    private static final ThreadLocal<Values> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(Values values) {
        CURRENT.set(values);
    }

    public static Values get() {
        return CURRENT.get();
    }

    public static void setSagaId(String sagaId) {
        Values current = CURRENT.get();
        if (current == null) {
            CURRENT.set(new Values(null, sagaId, null, null, false));
            return;
        }
        CURRENT.set(new Values(
                current.correlationId(),
                sagaId,
                current.delay(),
                current.forceFailure(),
                current.simulationEnabled()));
    }

    public static String correlationId() {
        Values values = CURRENT.get();
        return values == null ? null : values.correlationId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
