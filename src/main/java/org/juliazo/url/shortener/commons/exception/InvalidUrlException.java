package org.juliazo.url.shortener.commons.exception;

public class InvalidUrlException extends IllegalArgumentException {

    /**
     * Instantiates a new Invalid URL exception.
     * This exception occurs when the long url received by the service is null, empty or violates
     * any constraint given by RFC 2396.
     */
    public InvalidUrlException(Throwable throwable) {
        super("The provided url is malformed or otherwise invalid.", throwable);
    }
}
