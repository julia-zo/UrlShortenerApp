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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
public class UrlShortenerController {

    /**
     * The Shortener Service. Implementation of each endpoint mapped here.
     */
    private final UrlShortenerService shortenerService;

    /**
     * The constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);

    /**
     * Instantiates a new Url Shortener controller.
     *
     * @param shortenerService the url shortener service
     */
    @Autowired
    public UrlShortenerController(UrlShortenerService shortenerService) {
        this.shortenerService = shortenerService;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/shorten", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UrlResponsePayload> shortenUrl(@RequestBody final UrlRequestPayload urlRequestPayload) {
        String longUrl = urlRequestPayload.getLongUrl();
        logger.info("Controller: Attempting to create short url");
        String shortUrl = shortenerService.shortenUrl(longUrl);

        logger.info("Controller: Created short url: {}", shortUrl);
        UriComponents uriComponents =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("localhost:8080").path(shortUrl)
                        .build();
        UrlResponsePayload responsePayload = new UrlResponsePayload(longUrl, uriComponents.toUriString());
        return new ResponseEntity<>(responsePayload, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{shortUrl}")
    public ModelAndView lookupUrl(@PathVariable("shortUrl") final String shortUrl) {
        logger.info("Controller: Redirecting from short url: {} ", shortUrl);
        URI redirect = shortenerService.lookupUrl(shortUrl);

        logger.info("Controller: Redirecting to long url: {}", redirect.toString());
        UriComponents uriComponents =
                UriComponentsBuilder.newInstance().uri(redirect).build();
        return new ModelAndView("redirect:" + uriComponents.toUriString());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponsePayload> handleNotFoundError(final ResourceNotFoundException exception) {
        logger.error("ERROR: short url not found.");
        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), exception.getMessage());
        return new ResponseEntity<>(errorResponsePayload, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidUrlException.class)
    ResponseEntity<ErrorResponsePayload> handleInvalidUrlError(final InvalidUrlException exception) {
        logger.error("ERROR: malformed url, or otherwise invalid");
        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), exception.getMessage());
        return new ResponseEntity<>(errorResponsePayload, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictingDataException.class)
    ResponseEntity<ErrorResponsePayload> handleShortUrlConflictError(final ConflictingDataException exception) {
        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage());
        return new ResponseEntity<>(errorResponsePayload, HttpStatus.CONFLICT);
    }
}
