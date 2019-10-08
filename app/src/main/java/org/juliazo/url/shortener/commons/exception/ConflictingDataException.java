package org.juliazo.url.shortener.commons.exception;

public class ConflictingDataException extends RuntimeException {

    /**
     * Instantiates a new Conflicting Data exception.
     */
    public ConflictingDataException() {
        super("All attempts to create a short url resulted in conflicts, please try a different URL");
    }
}
