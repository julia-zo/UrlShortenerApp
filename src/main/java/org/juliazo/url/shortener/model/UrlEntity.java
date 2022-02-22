package org.juliazo.url.shortener.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Column;
import java.io.Serializable;

/**
 * DTO representing the pair short url - long url
 * Both values must be unique.
 */
@Entity
@Table(name = "url_entity")
@IdClass(UrlEntity.class)
public class UrlEntity implements Serializable {

    @Column(unique = true)
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

}
