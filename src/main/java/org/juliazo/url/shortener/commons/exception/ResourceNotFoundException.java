package org.juliazo.url.shortener.commons.exception;

public class ResourceNotFoundException extends RuntimeException {
    /**
     * Instantiates a new Resource Not Found exception.
     */
    public ResourceNotFoundException() {
        super("The requested short url was not found.");
    }
}
