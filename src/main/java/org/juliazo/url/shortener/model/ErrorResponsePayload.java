package org.juliazo.url.shortener.model;

public class ErrorResponsePayload {

    private int status;

    private String reasonPhrase;

    private String message;

    public ErrorResponsePayload(int status, String reasonPhrase, String message) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
