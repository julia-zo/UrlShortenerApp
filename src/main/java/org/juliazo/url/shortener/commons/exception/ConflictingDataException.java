package org.juliazo.url.shortener.commons.exception;

public class ConflictingDataException extends RuntimeException {

    /**
     * Instantiates a new Conflicting Data exception.
     * This exception occurs when all attempts to create a short url based on the hash of
     * the long url have resulted in conflict in the database.
     */
    public ConflictingDataException() {
        super("All attempts to create a short url resulted in conflicts, please try a different URL.");
    }
}
