package org.juliazo.url.shortener.repository;

import org.juliazo.url.shortener.model.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlShortenerRepository extends JpaRepository<UrlEntity, String> {

    List<UrlEntity> findByLongUrl(String longUrl);

    List<UrlEntity> findByShortUrl(String shortUrl);
}

