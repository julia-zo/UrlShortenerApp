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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
class UrlShortenerControllerTest {

    /**
     * The controller responsible for handling REST requests
     */
    @InjectMocks
    private UrlShortenerController urlShortenerController;

    /**
     * The service which holds the core logic for the application
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

        String shortUrl = "6e8b9a";
        String composedShortUrl = "http://localhost:80/" + shortUrl;
        when(urlShortenerService.shortenUrl(eq(requestPayload.getLongUrl()))).thenReturn(shortUrl);

        ResponseEntity<UrlResponsePayload> expected = new ResponseEntity<> (new UrlResponsePayload(requestPayload.getLongUrl(), composedShortUrl), HttpStatus.OK);

        ResponseEntity<UrlResponsePayload>  actual = urlShortenerController.shortenUrl(requestPayload);

        UrlResponsePayload actualPayload = actual.getBody();

        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertNotNull(actualPayload);
        assertEquals(requestPayload.getLongUrl(), actualPayload.getLongUrl());
        assertEquals(composedShortUrl, actualPayload.getShortUrl());
    }

    /**
     * Test: Send a valid short url to /{shortUrl }, the lookup endpoint, receive a long url in response
     */
    @Test
    public void testLookupUrl () {
        String shortUrl = "6e8b9a";
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
        InvalidUrlException exception = new InvalidUrlException(new Throwable());

        ErrorResponsePayload expectedErrorResponsePayload = new ErrorResponsePayload(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), exception.getMessage());
        ResponseEntity<ErrorResponsePayload> expected = new ResponseEntity<> (expectedErrorResponsePayload, HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorResponsePayload> actual = UrlShortenerController.handleInvalidUrlError(exception);

        ErrorResponsePayload actualPayload = actual.getBody();
        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertNotNull(actualPayload);
        assertEquals(expectedErrorResponsePayload.getMessage(), actualPayload.getMessage());
        assertEquals(expectedErrorResponsePayload.getStatus(), actualPayload.getStatus());
        assertEquals(expectedErrorResponsePayload.getReasonPhrase(), actualPayload.getReasonPhrase());
    }

    /**
     * Test: Check correctly formatted response when a ResourceNotFoundException is raised
     */
    @Test
    public void shouldHandleResourceNotFoundException () {
        ResourceNotFoundException exception = new ResourceNotFoundException();

        ErrorResponsePayload expectedErrorResponsePayload = new ErrorResponsePayload(HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), exception.getMessage());
        ResponseEntity<ErrorResponsePayload> expected = new ResponseEntity<>(expectedErrorResponsePayload, HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponsePayload> actual = UrlShortenerController.handleNotFoundError(exception);

        ErrorResponsePayload actualPayload = actual.getBody();
        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertNotNull(actualPayload);
        assertEquals(expectedErrorResponsePayload.getMessage(), actualPayload.getMessage());
        assertEquals(expectedErrorResponsePayload.getStatus(), actualPayload.getStatus());
        assertEquals(expectedErrorResponsePayload.getReasonPhrase(), actualPayload.getReasonPhrase());
    }

    /**
     * Test: Check correctly formatted response when a ConflictingDataException is raised
     */
    @Test
    public void shouldHandleConflictingDataException () {
        ConflictingDataException exception = new ConflictingDataException();

        ErrorResponsePayload expectedErrorResponsePayload = new ErrorResponsePayload(HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage());
        ResponseEntity<ErrorResponsePayload> expected = new ResponseEntity<>(expectedErrorResponsePayload, HttpStatus.CONFLICT);

        ResponseEntity<ErrorResponsePayload> actual = UrlShortenerController.handleShortUrlConflictError(exception);

        ErrorResponsePayload actualPayload = actual.getBody();
        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertNotNull(actualPayload);
        assertEquals(expectedErrorResponsePayload.getMessage(), actualPayload.getMessage());
        assertEquals(expectedErrorResponsePayload.getStatus(), actualPayload.getStatus());
        assertEquals(expectedErrorResponsePayload.getReasonPhrase(), actualPayload.getReasonPhrase());
    }

}