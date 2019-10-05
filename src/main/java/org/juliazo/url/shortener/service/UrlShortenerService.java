package org.juliazo.url.shortener.service;

import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.repository.UrlShortenerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class UrlShortenerService {

    /**
     * The constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);

    private final UrlShortenerRepository urlShortenerRepository = new UrlShortenerRepository();

    public String shortenUrl(final String longUrl) {
        URI validUri = validateUrl(longUrl);
        String urlHash = DigestUtils.md5DigestAsHex(validUri.toString().getBytes());

        //use the first 6 characters of the hash as short url
        String shortUrl = urlHash.substring(0, 6);

        if (!validUri.isAbsolute()) {
            validUri = URI.create("http://" + longUrl);
        }

        logger.info("Service: Creating short url");
        URI storedUrl = urlShortenerRepository.storeOrGet(shortUrl, validUri);

        return handleConflicts(validUri, storedUrl, urlHash);
    }

    private String handleConflicts(final URI longUrl, final URI storedUrl, final String urlHash) {
        int attempts = 1;
        String shortUrl = urlHash.substring(0, 6);
        URI retrievedUrl = storedUrl;
        while (!retrievedUrl.equals(longUrl) && attempts <= 10) {
            logger.info("Service: Conflict detected on short url, solving attempt: {}", attempts);
            //roll forward to get the next 6 chars
            shortUrl = urlHash.substring(attempts, attempts + 6);
            retrievedUrl = urlShortenerRepository.storeOrGet(shortUrl, longUrl);
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
            return new URI(longUrl);
        } catch (URISyntaxException e) {
            logger.error("ERROR: malformed url, or otherwise invalid");
            throw new InvalidUrlException();
        }
    }

    public URI lookupUrl(final String shortUrl) {
        if (shortUrl.length() == 6) {
            URI longUrl = urlShortenerRepository.lookupUrl(shortUrl);
            if (longUrl != null) {
                return longUrl;
            }
        }
        logger.error("ERROR: short url not found.");
        throw new ResourceNotFoundException();
    }


}
