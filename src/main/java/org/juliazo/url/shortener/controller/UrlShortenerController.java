package org.juliazo.url.shortener.controller;

import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.model.ErrorResponsePayload;
import org.juliazo.url.shortener.model.UrlRequestPayload;
import org.juliazo.url.shortener.model.UrlResponsePayload;
import org.juliazo.url.shortener.service.UrlShortenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Url Shortener Controller. Responsible for handling HTTP requests.
 * All endpoints mapped to the application must be defined here.
 */
@RestController
public class UrlShortenerController {

    private static final String URL_SCHEMA = "http";
    private static final String SERVICE_HOST = "localhost";
    private static final String SERVICE_PORT = "80";

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);

    /**
     * The Shortener Service. Implementation of each endpoint mapped here.
     */
    private final UrlShortenerService shortenerService;

    @Autowired
    public UrlShortenerController(UrlShortenerService shortenerService) {
        this.shortenerService = shortenerService;
    }

    /**
     * POST endpoint to create an alias for a given url, known as short url.
     *
     * @param urlRequestPayload holds the url to be shortened
     * @return an absolute url with the short alias as a path parameter
     */
    @RequestMapping(method = RequestMethod.POST, value = "/shorten", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UrlResponsePayload> shortenUrl(@RequestBody UrlRequestPayload urlRequestPayload) {
        String longUrl = urlRequestPayload.getLongUrl();
        logger.info("Attempting to create short url for [{}]", longUrl);
        String shortUrl = shortenerService.shortenUrl(longUrl);

        UriComponents uriComponents =
                UriComponentsBuilder.newInstance()
                        .scheme(URL_SCHEMA).host(SERVICE_HOST + ':' + SERVICE_PORT).path(shortUrl)
                        .build();
        UrlResponsePayload responsePayload = new UrlResponsePayload(longUrl, uriComponents.toUriString());
        return new ResponseEntity<>(responsePayload, HttpStatus.OK);
    }

    /**
     * GET endpoint to exchange a short url alias for its corresponding long url
     *
     * @param shortUrl alias for a given url
     * @return a 302 FOUND status code, redirecting the user to the correct url
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{shortUrl}")
    public ModelAndView lookupUrl(@PathVariable("shortUrl") String shortUrl) {
        logger.info("Redirecting from short url [{}] ", shortUrl);
        URI redirect = shortenerService.lookupUrl(shortUrl);

        logger.info("Redirecting to long url [{}]", redirect.toString());
        UriComponents uriComponents =
                UriComponentsBuilder.newInstance().uri(redirect).build();
        return new ModelAndView("redirect:" + uriComponents.toUriString());
    }

    /**
     * Exception handler for cases when the short url alias passed to {@link #lookupUrl} endpoint has no
     * corresponding long url in the database.
     *
     * @param exception {@link ResourceNotFoundException}
     * @return 404 NOT FOUND status code
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    static ResponseEntity<ErrorResponsePayload> handleNotFoundError(ResourceNotFoundException exception) {
        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), exception.getMessage());
        return new ResponseEntity<>(errorResponsePayload, HttpStatus.NOT_FOUND);
    }

    /**
     * Exception handler for cases when the long url passed to {@link #shortenUrl} does not comply with
     * RFC 2396 or when no long url was passed.
     *
     * @param exception {@link InvalidUrlException}
     * @return 400 BAD REQUEST status code
     */
    @ExceptionHandler(InvalidUrlException.class)
    static ResponseEntity<ErrorResponsePayload> handleInvalidUrlError(InvalidUrlException exception) {
        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), exception.getMessage());
        return new ResponseEntity<>(errorResponsePayload, HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler for cases when it was not possible to create a short url alias for the long url passed
     * to {@link #shortenUrl} due to the excessive number of conflicting records in the database.
     *
     * @param exception {@link ConflictingDataException}
     * @return 409 CONFLICT status code
     */
    @ExceptionHandler(ConflictingDataException.class)
    static ResponseEntity<ErrorResponsePayload> handleShortUrlConflictError(ConflictingDataException exception) {
        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage());
        return new ResponseEntity<>(errorResponsePayload, HttpStatus.CONFLICT);
    }
}
