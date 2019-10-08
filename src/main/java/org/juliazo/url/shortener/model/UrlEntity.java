package org.juliazo.url.shortener.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;
import java.util.Objects;

@Entity
@IdClass(UrlEntity.class)
public class UrlEntity implements Serializable {

    @Id
    private String shortUrl;

    @Id
    private String longUrl;

    public UrlEntity(String shortUrl, String longUrl) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
    }

    public UrlEntity() {
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlEntity urlEntity = (UrlEntity) o;
        return shortUrl.equals(urlEntity.shortUrl) &&
                longUrl.equals(urlEntity.longUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortUrl, longUrl);
    }
}
