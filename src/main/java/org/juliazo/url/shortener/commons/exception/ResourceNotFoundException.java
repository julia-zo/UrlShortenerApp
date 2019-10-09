package org.juliazo.url.shortener.commons.exception;

public class ResourceNotFoundException extends RuntimeException {
    /**
     * Instantiates a new Resource Not Found exception.
     * This exception occurs when the provided short url has no matching record in the database.
     */
    public ResourceNotFoundException() {
        super("The requested short url was not found.");
    }
}
