package com.pruebatecnica.platform.error;

public class RemoteDependencyException extends RuntimeException {

    private final String dependency;
    private final int status;

    public RemoteDependencyException(String dependency, int status, String message) {
        super(message);
        this.dependency = dependency;
        this.status = status;
    }

    public RemoteDependencyException(String dependency, int status, String message, Throwable cause) {
        super(message, cause);
        this.dependency = dependency;
        this.status = status;
    }

    public String dependency() {
        return dependency;
    }

    public int status() {
        return status;
    }
}
