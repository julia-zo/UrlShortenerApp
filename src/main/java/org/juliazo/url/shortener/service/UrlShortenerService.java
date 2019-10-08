package org.juliazo.url.shortener.service;

import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.model.UrlEntity;
import org.juliazo.url.shortener.repository.UrlShortenerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public
class UrlShortenerService {

    /**
     * The constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);
    public static final int SHORT_URL_SIZE = 6;

    @Autowired
    private UrlShortenerRepository urlShortenerRepository;

    public String shortenUrl(final String longUrl) {
        String validUrl = validateUrl(longUrl).toString();
        List<UrlEntity> foundEntities = urlShortenerRepository.findByUrlEntitybyLongUrl(validUrl);
        UrlEntity foundUrl;
        if (foundEntities == null || foundEntities.isEmpty()) {
            String shortUrl = generateShortUrl(validUrl, 0);
            UrlEntity newUrl = new UrlEntity(shortUrl, validUrl);
            foundUrl = urlShortenerRepository.save(newUrl);
        } else {
            foundUrl = foundEntities.get(0);
            if (foundUrl.getLongUrl().equals(validUrl)) {
                return foundEntities.get(0).getShortUrl();
            }
        }
        return handleConflicts(validUrl, foundUrl);
    }

    private String generateShortUrl(String validUri, int beginIndex) {
        String urlHash = DigestUtils.md5DigestAsHex(validUri.getBytes());
        //use the first 6 characters of the hash as short url
        return urlHash.substring(beginIndex, beginIndex + SHORT_URL_SIZE);
    }

    private String handleConflicts(final String longUrl, final UrlEntity storedUrlEntity) {
        int attempts = 1;
        String shortUrl = storedUrlEntity.getShortUrl();
        String retrievedUrl = storedUrlEntity.getLongUrl();
        while (!retrievedUrl.equals(longUrl) && attempts <= 10) {
            logger.info("Service: Conflict detected on short url, solving attempt: {}", attempts);
            //roll forward to get the next 6 chars
            shortUrl = generateShortUrl(longUrl, attempts);
            UrlEntity newUrl = new UrlEntity(shortUrl, longUrl);
            UrlEntity foundUrl = urlShortenerRepository.save(newUrl);
            retrievedUrl = foundUrl.getLongUrl();
            attempts += 1;
        }

        if (attempts > 10) {
            logger.error("ERROR: Unsolvable conflict. Unable to create short url for: {}", longUrl);
            throw new ConflictingDataException();
        }

        logger.info("Service: Short url created: {}", shortUrl);
        return shortUrl;
    }

    private URI validateUrl(final String longUrl) {
        try {
            String absoluteUrl = longUrl;
            if (!longUrl.startsWith("http")) {
                absoluteUrl = "http://" + longUrl;
            }
            return new URI(absoluteUrl);
        } catch (URISyntaxException | NullPointerException e) {
            throw new InvalidUrlException();
        }
    }

    public URI lookupUrl(final String shortUrl) {
        if (shortUrl.length() == 6) {
            List<UrlEntity> foundUrl = urlShortenerRepository.findByUrlEntitybyShortUrl(shortUrl);
            if (foundUrl != null && !foundUrl.isEmpty()) {
                URI longUrl = URI.create(foundUrl.get(0).getLongUrl());
                return longUrl;
            }
        }
        throw new ResourceNotFoundException();
    }


}
