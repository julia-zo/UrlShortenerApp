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
import org.springframework.dao.DataIntegrityViolationException;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
     * All long urls must be absolute (start with http or https).
     * Relative urls are interpreted as being relative to this service,
     * not separate urls.
     *
     * @param longUrl
     * @return the url with http protocol
     */
    private static String makeAbsoluteUrl(String longUrl) {
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

        Optional<UrlEntity> foundEntity = Optional.of(new UrlEntity(shortUrl, longUrl.toString()));
        when(urlShortenerRepository.findByShortUrl(eq(shortUrl))).thenReturn(foundEntity);
        URI actual = urlShortenerService.lookupUrl(shortUrl);

        assertEquals(longUrl.toString(), actual.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"elu39", "balling", "julia1", "", " "})
    public void testLookupInvalidUrl(String shortUrl) {
        assertThrows(ResourceNotFoundException.class, () -> urlShortenerService.lookupUrl(shortUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://google.com", "http://www.google.com,", "google.com", "httpforever.com",
            "https://www.google.com/search?q=Grandparents%27+Day&oi=ddle&ct=119275999&hl=en-GB&sa=X&ved=0ahUKEwi8rY3qvIflAhWPRMAKHXkaDJsQPQgL&biw=1191&bih=634&dpr=1"})
    public void testShortenValidUrl(String longUrl) {
        when(urlShortenerRepository.save(any(UrlEntity.class))).thenReturn(new UrlEntity(RandomString.make(6), makeAbsoluteUrl(longUrl)));

        String shortUrl = urlShortenerService.shortenUrl(longUrl);
        assertNotNull(shortUrl);
        assertFalse(shortUrl.isEmpty());
    }

    @Test
    public void testShortenExistentValidUrl() {
        String longUrl = "https://www.ea.com/frostbite/engine";

        Optional<UrlEntity> foundEntity = Optional.of(new UrlEntity("6e8b9a", longUrl));
        when(urlShortenerRepository.findByLongUrl(any())).thenReturn(foundEntity);

        String shortUrl = urlShortenerService.shortenUrl(longUrl);
        assertEquals("6e8b9a", shortUrl);
    }

    @Test
    public void testShortenValidUrlAfterConflict() {
        String longUrl = "https://www.ea.com/frostbite/engine";
        Optional<UrlEntity> foundEntity = Optional.of(new UrlEntity("6e8b9a", longUrl));

        //Step 1: no entity found, Step 3: entity found
        when(urlShortenerRepository.findByLongUrl(any())).thenReturn(Optional.empty(), foundEntity);
        //Step 2: conflict simulating two threads saving the same entity at the same time
        when(urlShortenerRepository.save(any())).thenThrow(new DataIntegrityViolationException("Conflict"));

        String shortUrl = urlShortenerService.shortenUrl(longUrl);
        assertEquals("6e8b9a", shortUrl);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "http://goo gle.com", "goog|e.com"})
    public void testShortenInvalidUrl(String longUrl) {
        assertThrows(InvalidUrlException.class, () -> urlShortenerService.shortenUrl(longUrl));
    }

    @Test
    public void testShortenUrlWithTooManyConflicts() {
        String longUrl = "https://www.ea.com/frostbite/engine";

        when(urlShortenerRepository.findByLongUrl(any())).thenReturn(Optional.empty());
        when(urlShortenerRepository.save(any())).thenThrow(new DataIntegrityViolationException("Conflict"));

        assertThrows(ConflictingDataException.class, () -> urlShortenerService.shortenUrl(longUrl));
    }
}