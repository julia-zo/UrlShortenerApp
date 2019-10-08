package org.juliazo.url.shortener;

import org.juliazo.url.shortener.controller.UrlShortenerController;
import org.juliazo.url.shortener.model.ErrorResponsePayload;
import org.juliazo.url.shortener.model.UrlRequestPayload;
import org.juliazo.url.shortener.model.UrlResponsePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Url Shortener Application
 * To run only integration tests use command line:
 * mvn clean test -Dtest=UrlShortenerAppIntTest
 * <p>
 * To run all tests except this one (only unit tests)
 * use command line:
 * mvn clean test -Dtest=\!UrlShortenerAppIntTest
 * <p>
 * Coverage report will be in ./target/jacoco-coverage/index.html
 */
@RunWith(JUnitPlatform.class)
@SpringBootTest(classes = UrlShortenerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UrlShortenerAppIntTest {

    public static final int SHORT_URL_SIZE = 6;
    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private final HttpHeaders headers = new HttpHeaders();

    /**
     * Controller to whom SpringBoot will assign the context;
     */
    @Autowired
    private UrlShortenerController urlShortenerController;

    /**
     * Creates the service URL using localhost and a dynamic port provided by Springboot
     *
     * @param uri the query parameters and endpoint to be used
     * @return the compound URI
     */
    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + "/" + uri;
    }

    /**
     * POST Operation to shorten a valid url
     *
     * @param requestPayload the input data as a {@link org.juliazo.url.shortener.model.UrlRequestPayload}
     * @return the response of the request as a {@link org.juliazo.url.shortener.model.UrlResponsePayload}
     */
    private ResponseEntity<UrlResponsePayload> shortenValidUrl(UrlRequestPayload requestPayload) {
        HttpEntity<UrlRequestPayload> entity = new HttpEntity<>(requestPayload, headers);

        return restTemplate.exchange(
                createURLWithPort("shorten"),
                HttpMethod.POST, entity, UrlResponsePayload.class);
    }

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

    @ParameterizedTest
    @ValueSource(strings = {"https://google.com", "https://www.google.com,",
            "http://google.com", "http://www.google.com", "google.com", "www.google.com",
            "https://www.google.com/search?q=Grandparents%27+Day&oi=ddle&ct=119275999&hl=en-GB&sa=X&ved=0ahUKEwi8rY3qvIflAhWPRMAKHXkaDJsQPQgL&biw=1191&bih=634&dpr=1"})
    public void testShortenValidUrl(String longUrl) {
        System.out.println("Running test for url: " + longUrl);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl);

        ResponseEntity<UrlResponsePayload> response = shortenValidUrl(requestPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UrlResponsePayload responsePayload = response.getBody();
        assertEquals(longUrl, responsePayload.getLongUrl());
        var shortUrl = responsePayload.getShortUrl();
        assertNotNull(shortUrl);
        assertTrue(!shortUrl.isEmpty());
    }

    /**
     * Test POST /shorten for the same url
     * with or without http protocol = same.
     */
    @Test
    public void testShortenTheSameValidUrl() {
        var longUrl = "http://google.com";
        UrlRequestPayload requestPayload1 = new UrlRequestPayload();
        requestPayload1.setLongUrl(longUrl);
        UrlRequestPayload requestPayload2 = new UrlRequestPayload();
        requestPayload2.setLongUrl("google.com");

        ResponseEntity<UrlResponsePayload> response1 = shortenValidUrl(requestPayload1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        ResponseEntity<UrlResponsePayload> response2 = shortenValidUrl(requestPayload2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        var shortUrl1 = response1.getBody().getShortUrl();
        var shortUrl2 = response2.getBody().getShortUrl();

        assertNotNull(shortUrl1);
        assertTrue(!shortUrl1.isEmpty());
        assertNotNull(shortUrl2);
        assertTrue(!shortUrl2.isEmpty());
        assertEquals(shortUrl1, shortUrl2);
    }

    /**
     * Test POST /shorten for the same url
     * http and https protocols = different
     * with or without www = different
     */
    @Test
    public void testShortenDifferentValidUrl() {
        var longUrl = "http://google.com";
        UrlRequestPayload requestPayload1 = new UrlRequestPayload();
        requestPayload1.setLongUrl(longUrl);
        UrlRequestPayload requestPayload2 = new UrlRequestPayload();
        requestPayload2.setLongUrl("http://www.google.com");

        ResponseEntity<UrlResponsePayload> response1 = shortenValidUrl(requestPayload1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        ResponseEntity<UrlResponsePayload> response2 = shortenValidUrl(requestPayload2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        var shortUrl1 = response1.getBody().getShortUrl();
        var shortUrl2 = response2.getBody().getShortUrl();

        assertNotNull(shortUrl1);
        assertTrue(!shortUrl1.isEmpty());
        assertNotNull(shortUrl2);
        assertTrue(!shortUrl2.isEmpty());
        assertNotEquals(shortUrl1, shortUrl2);
    }

    /**
     * Test invalid cases for POST /shorten:
     * Null, empty string, invalid character.
     *
     * @param longUrl
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"","https://goo gle.com","http://goog|e.com"})
    public void testShortenInvalidUrl(String longUrl) {
        System.out.println("Running test for url: " + longUrl);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl);

        ResponseEntity<ErrorResponsePayload> response = restTemplate.exchange(
                createURLWithPort("shorten"), HttpMethod.POST,
                new HttpEntity<>(requestPayload, headers), ErrorResponsePayload.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponsePayload responsePayload = response.getBody();
        assertEquals(HttpStatus.BAD_REQUEST.value(), responsePayload.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), responsePayload.getReasonPhrase());
        assertEquals("The provided url is malformed or otherwise invalid.", responsePayload.getMessage());
    }

    /**
     * Test invalid cases for GET /{shortUrl}:
     * Shorter than 6 chars, bigger than 6 chars, non-existent.
     *
     * @param shortUrl
     */
    @ParameterizedTest
    @ValueSource(strings = {"elu39", "balling", "julia1"})
    public void testLookupInvalidUrl(String shortUrl) {
        System.out.println("Running test for url: " + shortUrl);

        ResponseEntity<ErrorResponsePayload> response = restTemplate.exchange(
                createURLWithPort(shortUrl), HttpMethod.GET,
                null, ErrorResponsePayload.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponsePayload responsePayload = response.getBody();
        assertEquals(HttpStatus.NOT_FOUND.value(), responsePayload.getStatus());

        assertEquals("The requested short url was not found.", responsePayload.getMessage());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), responsePayload.getReasonPhrase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://google.com", "www.google.com",
            "https://www.google.com/search?q=Grandparents%27+Day&oi=ddle&ct=119275999&hl=en-GB&sa=X&ved=0ahUKEwi8rY3qvIflAhWPRMAKHXkaDJsQPQgL&biw=1191&bih=634&dpr=1"})
    public void testLookupValidUrl(String longUrl) {
        System.out.println("Running test for url: " + longUrl);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl);

        ResponseEntity<UrlResponsePayload> response = shortenValidUrl(requestPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        var shortUrl = response.getBody().getShortUrl();
        shortUrl = shortUrl.substring(shortUrl.length()- SHORT_URL_SIZE);

        ResponseEntity actual = restTemplate.getForEntity(
                createURLWithPort(shortUrl),
                ResponseEntity.class);

        assertEquals(HttpStatus.FOUND, actual.getStatusCode());
        assertEquals(makeAbsoluteUrl(longUrl), actual.getHeaders().getLocation().toString());
    }
}