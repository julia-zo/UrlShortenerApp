package org.juliazo.url.shortener.commons.exception;

public class InvalidUrlException extends IllegalArgumentException {

    /**
     * Instantiates a new Invalid URL exception.
     */
    public InvalidUrlException() {
        super("The provided url is malformed or otherwise invalid.");
    }
}
