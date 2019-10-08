package org.juliazo.url.shortener.service;

import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.controller.UrlShortenerController;
import org.juliazo.url.shortener.model.UrlRequestPayload;
import org.juliazo.url.shortener.model.UrlResponsePayload;
import org.juliazo.url.shortener.repository.UrlShortenerRepository;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
class UrlShortenerServiceTest {

    /**
     * The service which holds the core logic for the application
     */
    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @Test
    public void testLookupValidUrl() {
        URI longUrl = URI.create("http://ea.com/frostbite");
        String shortUrl = "6e8b9a";

        urlShortenerService.urlShortenerRepository.urlStorage.putIfAbsent(shortUrl, longUrl);

        URI actual = urlShortenerService.lookupUrl(shortUrl);

        assertEquals(longUrl.toString(), actual.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"elu39", "balling", "julia1", ""})
    public void testLookupInValidUrl(String shortUrl) {
        assertThrows(ResourceNotFoundException.class, () -> {
            urlShortenerService.lookupUrl(shortUrl);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://google.com", "http://www.google.com,", "google.com",
            "https://www.google.com/search?q=Grandparents%27+Day&oi=ddle&ct=119275999&hl=en-GB&sa=X&ved=0ahUKEwi8rY3qvIflAhWPRMAKHXkaDJsQPQgL&biw=1191&bih=634&dpr=1"})
    public void testShortenValidUrl(String longUrl) {
        String shortUrl = urlShortenerService.shortenUrl(longUrl);
        assertNotNull(shortUrl);
        assertTrue(!shortUrl.isEmpty());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "http://goo gle.com", "goog|e.com"})
    public void testShortenInvalidUrl(String longUrl) {
        assertThrows(InvalidUrlException.class, () -> {
            urlShortenerService.shortenUrl(longUrl);
        });
    }

    @Test
    public void testShortenUrlWithOneConflict() {
        URI storedLongUrl = URI.create("ea.com/frostbite");
        String longUrl = "https://www.ea.com/frostbite/engine";
        String shortUrl = "6e8b9a";

        urlShortenerService.urlShortenerRepository.urlStorage.putIfAbsent(shortUrl, storedLongUrl);

        String actualUrl = urlShortenerService.shortenUrl(longUrl);
        assertNotNull(actualUrl);
        assertTrue(!actualUrl.isEmpty());
        assertNotEquals(shortUrl, actualUrl);
    }

    @Test
    public void testShortenUrlWithTooManyConflict () {
        URI storedLongUrl = URI.create("ea.com/frostbite");
        String longUrl = "https://www.ea.com/frostbite/engine";

        String hashUrl = "6e8b9a8a8314b8c2d822f8706fd57b3c";
        for (int i = 0; i<10; i++) {
            String shortUrl = hashUrl.substring(i, i+6);
            urlShortenerService.urlShortenerRepository.urlStorage.putIfAbsent(shortUrl, storedLongUrl);
        }

        assertThrows(ConflictingDataException.class, () -> {
            urlShortenerService.shortenUrl(longUrl);
        });
    }
}