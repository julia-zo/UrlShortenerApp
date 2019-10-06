package org.juliazo.url.shortener.model;

import java.util.Objects;

public class UrlResponsePayload {

    private String longUrl;

    private String shortUrl;

    public UrlResponsePayload(String longUrl, String shortUrl) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
    }

    public UrlResponsePayload() {
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlResponsePayload that = (UrlResponsePayload) o;
        return longUrl.equals(that.longUrl) &&
                shortUrl.equals(that.shortUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longUrl, shortUrl);
    }
}
