package com.pruebatecnica.platform.error;

public class CircuitOpenException extends RuntimeException {

    private final String dependency;

    public CircuitOpenException(String dependency, Throwable cause) {
        super("Circuito abierto hacia " + dependency, cause);
        this.dependency = dependency;
    }

    public String dependency() {
        return dependency;
    }
}
