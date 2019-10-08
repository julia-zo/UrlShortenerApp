package org.juliazo.url.shortener.service;

import net.bytebuddy.utility.RandomString;
import org.juliazo.url.shortener.commons.exception.ConflictingDataException;
import org.juliazo.url.shortener.commons.exception.InvalidUrlException;
import org.juliazo.url.shortener.commons.exception.ResourceNotFoundException;
import org.juliazo.url.shortener.model.UrlEntity;
import org.juliazo.url.shortener.repository.UrlShortenerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

    @Mock
    private UrlShortenerRepository urlShortenerRepository;

    /**
     * All long urls must be absolute (start with http or https) for the
     * redirect in the ModelAndView class to work.
     * Relative urls are interpreted as being relative to this service,
     * not separate urls.
     *
     * @param longUrl
     * @return the url with http protocol
     */
    private String makeAbsoluteUrl(String longUrl) {
        String absoluteUrl = longUrl;
        if (!longUrl.startsWith("http")) {
            absoluteUrl = "http://" + longUrl;
        }
        return absoluteUrl;
    }

    @Test
    public void testLookupValidUrl() {
        URI longUrl = URI.create("http://ea.com/frostbite");
        String shortUrl = "6e8b9a";

        List<UrlEntity> foundUrls = new ArrayList<>();
        foundUrls.add(new UrlEntity(shortUrl, longUrl.toString()));
        when(urlShortenerRepository.findByUrlEntitybyShortUrl(eq(shortUrl))).thenReturn(foundUrls);
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
        when(urlShortenerRepository.save(any(UrlEntity.class))).thenReturn(new UrlEntity(RandomString.make(6), makeAbsoluteUrl(longUrl)));

        String shortUrl = urlShortenerService.shortenUrl(longUrl);
        assertNotNull(shortUrl);
        assertTrue(!shortUrl.isEmpty());
    }

    @Test
    public void testShortenExistentValidUrl() {
        String longUrl = "https://www.ea.com/frostbite/engine";

        List<UrlEntity> foundEntities = new ArrayList<>();
        foundEntities.add(new UrlEntity("6e8b9a", longUrl));
        when(urlShortenerRepository.findByUrlEntitybyLongUrl(any())).thenReturn(foundEntities);

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
        String storedLongUrl = "http://ea.com/frostbite";
        String longUrl = "https://www.ea.com/frostbite/engine";
        String shortUrl = "6e8b9a";

        List<UrlEntity> foundEntities = new ArrayList<>();
        foundEntities.add(new UrlEntity(shortUrl, storedLongUrl));
        when(urlShortenerRepository.findByUrlEntitybyLongUrl(any())).thenReturn(foundEntities);
        when(urlShortenerRepository.save(any(UrlEntity.class))).thenReturn(new UrlEntity(RandomString.make(6), longUrl));

        String actualUrl = urlShortenerService.shortenUrl(longUrl);
        assertNotNull(actualUrl);
        assertTrue(!actualUrl.isEmpty());
        assertNotEquals(shortUrl, actualUrl);
    }

    @Test
    public void testShortenUrlWithTooManyConflict() {
        String storedLongUrl = "http://ea.com/frostbite";
        String longUrl = "https://www.ea.com/frostbite/engine";
        String hashUrl = "6e8b9a8a8314b8c2d822f8706fd57b3c";
        String shortUrl = hashUrl.substring(0, 6);

        List<UrlEntity> foundEntities = new ArrayList<>();
        UrlEntity storedUrl = new UrlEntity(shortUrl, storedLongUrl);
        foundEntities.add(storedUrl);
        when(urlShortenerRepository.findByUrlEntitybyLongUrl(any())).thenReturn(foundEntities);
        when(urlShortenerRepository.save(any(UrlEntity.class))).thenReturn(storedUrl);

        assertThrows(ConflictingDataException.class, () -> {
            urlShortenerService.shortenUrl(longUrl);
        });
    }
}