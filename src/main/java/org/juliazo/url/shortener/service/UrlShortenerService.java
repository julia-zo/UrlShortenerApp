package org.juliazo.url.shortener.service;

import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.model.UrlEntity;
import org.juliazo.url.shortener.repository.UrlShortenerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public
class UrlShortenerService {

    public static final int SHORT_URL_SIZE = 6;
    private static final int MAX_CONFLICT_SOLVING_ATTEMPTS = 10;
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);

    @Autowired
    private UrlShortenerRepository urlShortenerRepository;

    public String shortenUrl(final String longUrl) {
        String validUrl = validateUrl(longUrl).toString();
        List<UrlEntity> foundEntities = urlShortenerRepository.findByLongUrl(validUrl);

        if (foundEntities == null || foundEntities.isEmpty()) {
            return createAndSaveShortUrl(validUrl, 0);
        }
        String foundShortUrl = foundEntities.get(0).getShortUrl();
        logger.info("Found longUrl [{}], using shortUrl [{}] from database", validUrl, foundShortUrl);
        return foundShortUrl;
    }

    private String generateShortUrl(String validUrl, int beginIndex) {
        String urlHash = DigestUtils.md5DigestAsHex(validUrl.getBytes());
        return urlHash.substring(beginIndex, beginIndex + SHORT_URL_SIZE);
    }

    private String createAndSaveShortUrl(String validUrl, int beginIndex) {
        String shortUrl = generateShortUrl(validUrl, beginIndex);
        UrlEntity newUrl = new UrlEntity(shortUrl, validUrl);
        try {
            UrlEntity foundUrl = urlShortenerRepository.save(newUrl);
            logger.info("Created shortUrl [{}] for longUrl [{}]", shortUrl, validUrl);
            return foundUrl.getShortUrl();
        } catch (DataIntegrityViolationException exception) {
            logger.info("Conflict detected for shortUrl [{}] + longUrl [{}]", shortUrl, validUrl);
            return handleConflicts(validUrl, beginIndex);
        }
    }

    private String handleConflicts(final String validUrl, final int beginIndex) {
        List<UrlEntity> conflictingEntities = urlShortenerRepository.findByLongUrl(validUrl);
        if (conflictingEntities != null && !conflictingEntities.isEmpty()) {
            String foundShortUrl = conflictingEntities.get(0).getShortUrl();
            logger.info("Found longUrl[{}], using shortUrl [{}] from database", validUrl, foundShortUrl);
            return foundShortUrl;
        } else {
            if (beginIndex > MAX_CONFLICT_SOLVING_ATTEMPTS) {
                logger.error("Unsolvable conflict. Unable to create short url for [{}]", validUrl);
                throw new ConflictingDataException();
            }
            int index = beginIndex + 1;
            logger.info("Could not store longUrl [{}], attempt [{}] to create a new shortUrl", validUrl, index);
            return createAndSaveShortUrl(validUrl, index);
        }
    }

    private URI validateUrl(final String longUrl) {
        try {
            String absoluteUrl = longUrl;
            if (!longUrl.startsWith("http")) {
                absoluteUrl = "http://" + longUrl;
            }
            return new URI(absoluteUrl);
        } catch (URISyntaxException | NullPointerException e) {
            logger.info("Malformed url, or otherwise invalid");
            throw new InvalidUrlException();
        }
    }

    public URI lookupUrl(final String shortUrl) {
        if (shortUrl.length() == SHORT_URL_SIZE) {
            List<UrlEntity> foundUrl = urlShortenerRepository.findByShortUrl(shortUrl);
            if (foundUrl != null && !foundUrl.isEmpty()) {
                URI longUrl = URI.create(foundUrl.get(0).getLongUrl());
                return longUrl;
            }
        }
        logger.info("Could not find longUrl associated with shortUrl [{}]", shortUrl);
        throw new ResourceNotFoundException();
    }
}
