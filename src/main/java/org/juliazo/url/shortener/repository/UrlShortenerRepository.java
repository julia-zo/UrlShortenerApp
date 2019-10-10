package org.juliazo.url.shortener.repository;

import org.juliazo.url.shortener.model.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlShortenerRepository extends JpaRepository<UrlEntity, String> {

    Optional<UrlEntity> findByLongUrl(String longUrl);

    Optional<UrlEntity> findByShortUrl(String shortUrl);
}

