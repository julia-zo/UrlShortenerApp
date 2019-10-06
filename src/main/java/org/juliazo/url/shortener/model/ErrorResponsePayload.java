package org.juliazo.url.shortener.model;

import java.util.Objects;

public class ErrorResponsePayload {

    private int status;

    private String reasonPhrase;

    private String message;

    public ErrorResponsePayload(int status, String reasonPhrase, String message) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        this.message = message;
    }

    public ErrorResponsePayload() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponsePayload that = (ErrorResponsePayload) o;
        return status == that.status &&
                reasonPhrase.equals(that.reasonPhrase) &&
                message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, reasonPhrase, message);
    }
}
