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
import java.util.Optional;

/**
 * Business logic of the REST Service. This class holds the implementation of all
 * REST operations accepted by the Url Shortener Application.
 * <p>
 * In case the application gets too big and or complex, validation and auxiliary
 * methods can be moved to a separated class and package to improve readability.
 */
@Service
public class UrlShortenerService {

    private static final int SHORT_URL_SIZE = 6;
    private static final int DEFAULT_BEGIN_INDEX = 0;
    private static final int MAX_CONFLICT_SOLVING_ATTEMPTS = 10;
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);


    @Autowired
    private UrlShortenerRepository urlShortenerRepository;

    /**
     * Processes a POST request to /shorten which receives a long url
     * in the request payload.
     * <p>
     * Searches the database for a short url already associated with
     * the provided long url. If found, returns it to the caller. If
     * no corresponding short url is found, generates one and save
     * it to the database, returning it to the caller after a successful
     * storage.
     *
     * @param longUrl the long url from the request payload
     * @return a corresponding short url
     * @throws InvalidUrlException      in case the provided long url is invalid
     * @throws ConflictingDataException in case all the attempts to crate a
     *                                  short url did not result in a successful storage of the short url -
     *                                  long url pair.
     * @see #generateShortUrl
     * @see #createAndSaveShortUrl
     * @see #handleConflicts
     */
    public String shortenUrl(String longUrl) {
        String validUrl = validateUrl(longUrl);
        Optional<UrlEntity> foundEntities = urlShortenerRepository.findByLongUrl(validUrl);

        if (foundEntities.isEmpty()) {
            return createAndSaveShortUrl(validUrl, DEFAULT_BEGIN_INDEX);
        }
        String foundShortUrl = foundEntities.get().getShortUrl();
        logger.info("Found longUrl [{}], using shortUrl [{}] from database", validUrl, foundShortUrl);
        return foundShortUrl;
    }

    /**
     * Generates an MD5 hash string based on the provided valid long url.
     * Chooses a short url of {@link #SHORT_URL_SIZE} characters starting
     * from the provided {@code beginIndex}. The selection of the characters
     * is from left to right.
     *
     * @param validUrl   an already validated long url
     * @param beginIndex from where should the short url start in the url hash
     * @return the generated short url
     */
    private static String generateShortUrl(String validUrl, int beginIndex) {
        String urlHash = DigestUtils.md5DigestAsHex(validUrl.getBytes());
        return urlHash.substring(beginIndex, beginIndex + SHORT_URL_SIZE);
    }

    /**
     * Method to generate a short url and store the pair short url - long url
     * in the database in the form of {@link UrlEntity}.
     * <p>
     * The short url will be returned to the caller when a successful storage
     * occurs.
     * If the storage is not possible due to database constraints, a
     * call to {@link #handleConflicts} will be made in an attempt to solve
     * the conflict.
     *
     * @param validUrl   an already validated long url
     * @param beginIndex from where should the short url start in the url hash
     * @return the created short url after successful storage of the url pair
     */
    private String createAndSaveShortUrl(String validUrl, int beginIndex) {
        String shortUrl = generateShortUrl(validUrl, beginIndex);
        UrlEntity newUrl = new UrlEntity(shortUrl, validUrl);
        try {
            UrlEntity foundUrl = urlShortenerRepository.save(newUrl);
            logger.info("Created shortUrl [{}] for longUrl [{}]", shortUrl, validUrl);
            return foundUrl.getShortUrl();
        } catch (DataIntegrityViolationException exception) {
            logger.debug("Conflict detected for shortUrl [{}] + longUrl [{}]", shortUrl, validUrl);
            return handleConflicts(validUrl, beginIndex);
        }
    }

    /**
     * Method to analise {@link DataIntegrityViolationException} from the database.
     * This exceptions can happen in two cases:
     * 1: Multiple simultaneous requests attempted to add the same pair of short
     * url - long url, one was successful, the others resulted in conflict.
     * 2: The short url generated was already in use and associated with a different
     * long url. This hash conflict is more frequent than the expected for the chosen
     * algorithm because we are using only {@value SHORT_URL_SIZE} characters of the
     * resulting hash.
     * <p>
     * Resolutions:
     * 1: Read from the database the short url that matches the provided valid long url.
     * 2: Change the short url and retry the saving process. There is an arbitrary limit
     * of {@value MAX_CONFLICT_SOLVING_ATTEMPTS} for how many retries the system supports
     * before aborting the process.
     *
     * @param validUrl            an already validated long url
     * @param conflictingAttempts number of attempts to create a short url that resulted in conflict
     * @return a short url corresponding to the given valid long url
     * @throws ConflictingDataException when the number of {@code conflictingAttempts} exceeds
     *                                  {@value MAX_CONFLICT_SOLVING_ATTEMPTS}
     */
    private String handleConflicts(String validUrl, int conflictingAttempts) {
        Optional<UrlEntity> conflictingEntities = urlShortenerRepository.findByLongUrl(validUrl);
        if (conflictingEntities.isPresent()) {
            String foundShortUrl = conflictingEntities.get().getShortUrl();
            logger.info("Found longUrl[{}], using shortUrl [{}] from database", validUrl, foundShortUrl);
            return foundShortUrl;
        } else {
            if (conflictingAttempts > MAX_CONFLICT_SOLVING_ATTEMPTS) {
                logger.error("Unsolvable conflict. Unable to create short url for [{}]", validUrl);
                throw new ConflictingDataException();
            }
            int index = conflictingAttempts + 1;
            logger.debug("Could not store longUrl [{}], attempt [{}] to create a new shortUrl", validUrl, index);
            return createAndSaveShortUrl(validUrl, index);
        }
    }

    /**
     * Validates given url according to the RFC 2396.
     * Makes the url absolute by adding the http schema to
     * urls without any schema.
     *
     * @param longUrl the url to be validated
     * @return the absolute url, valid
     * @throws InvalidUrlException for invalid urls
     */
    private static String validateUrl(String longUrl) {
        try {
            String absoluteUrl = longUrl;
            if (!longUrl.startsWith("http")) {
                absoluteUrl = "http://" + longUrl;
            }
            return new URI(absoluteUrl).toString();
        } catch (URISyntaxException | NullPointerException e) {
            logger.info("Malformed url, or otherwise invalid");
            throw new InvalidUrlException(e.getCause());
        }
    }

    /**
     * Processes a GET request to /{shortUrl}, searching the database for a
     * corresponding {@link UrlEntity} based on the short url alias.
     *
     * @param shortUrl alias for the long url
     * @return the corresponding long url
     * @throws ResourceNotFoundException when there is no corresponding long url
     */
    public URI lookupUrl(String shortUrl) {
        if (shortUrl.length() == SHORT_URL_SIZE) {
            Optional<UrlEntity> foundUrl = urlShortenerRepository.findByShortUrl(shortUrl);
            if (foundUrl.isPresent()) {
                return URI.create(foundUrl.get().getLongUrl());
            }
        }
        logger.info("Could not find longUrl associated with shortUrl [{}]", shortUrl);
        throw new ResourceNotFoundException();
    }
}
