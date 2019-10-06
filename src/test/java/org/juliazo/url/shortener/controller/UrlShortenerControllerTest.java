package org.juliazo.url.shortener.controller;

import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.model.ErrorResponsePayload;
import org.juliazo.url.shortener.model.UrlRequestPayload;
import org.juliazo.url.shortener.model.UrlResponsePayload;
import org.juliazo.url.shortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class UrlShortenerControllerTest {

    /**
     * The controller responsible for handling REST requests
     */
    @InjectMocks
    private UrlShortenerController urlShortenerController;

    /**
     * The service that holds the entire functionality of the application
     */
    @Mock
    private UrlShortenerService urlShortenerService;

    /**
     * Test: Send a valid long url to /shorten endpoint, receive a short url in response
     */
    @Test
    public void testShortenUrl () {
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl("ea.com/frostbite");

        String shortUrl = "8uio2i";
        String composedShortUrl = "http://localhost:8080/" + shortUrl;
        when(urlShortenerService.shortenUrl(eq(requestPayload.getLongUrl()))).thenReturn(shortUrl);

        ResponseEntity expected = new ResponseEntity (new UrlResponsePayload(requestPayload.getLongUrl(), composedShortUrl), HttpStatus.OK);

        ResponseEntity  actual = urlShortenerController.shortenUrl(requestPayload);

        assertEquals(expected, actual);
    }

    /**
     * Test: Send a valid short url to /lookup endpoint, receive a long url in response
     */
    @Test
    public void testLookupUrl () {
        String shortUrl = "8uio2i";
        String composedLongUrl = "http://ea.com/frostbite";
        when(urlShortenerService.lookupUrl(eq(shortUrl))).thenReturn(URI.create(composedLongUrl));

        ModelAndView actual = urlShortenerController.lookupUrl(shortUrl);

        ModelAndViewAssert.assertViewName(actual, "redirect:" + composedLongUrl);
    }

    /**
     * Test: Check correctly formatted response when an InvalidUrlException is raised
     */
    @Test
    public void shouldHandleInvalidUrlException () {
        InvalidUrlException exception = new InvalidUrlException();

        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), exception.getMessage());
        ResponseEntity expected = new ResponseEntity(errorResponsePayload, HttpStatus.BAD_REQUEST);

        ResponseEntity actual = urlShortenerController.handleInvalidUrlError(exception);

        assertEquals(expected, actual);
    }

    /**
     * Test: Check correctly formatted response when a ResourceNotFoundException is raised
     */
    @Test
    public void shouldHandleResourceNotFoundlException () {
        ResourceNotFoundException exception = new ResourceNotFoundException();

        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), exception.getMessage());
        ResponseEntity expected = new ResponseEntity(errorResponsePayload, HttpStatus.NOT_FOUND);

        ResponseEntity actual = urlShortenerController.handleNotFoundError(exception);

        assertEquals(expected, actual);
    }

    /**
     * Test: Check correctly formatted response when a ConflictingDataException is raised
     */
    @Test
    public void shouldHandleConflictingDataException () {
        ConflictingDataException exception = new ConflictingDataException();

        ErrorResponsePayload errorResponsePayload = new ErrorResponsePayload(HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage());
        ResponseEntity expected = new ResponseEntity(errorResponsePayload, HttpStatus.CONFLICT);

        ResponseEntity actual = urlShortenerController.handleShortUrlConflictError(exception);

        assertEquals(expected, actual);
    }

}