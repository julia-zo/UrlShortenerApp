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
 * mvn clean tests -Dtest=\!UrlShortenerAppIntTest
 * <p>
 * Coverage report will be in ./target/jacoco-coverage/index.html
 */
@RunWith(JUnitPlatform.class)
@SpringBootTest(classes = UrlShortenerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UrlShortenerAppIntTest {

    @LocalServerPort
    private int port;

    /**
     * The Rest template.
     */
    private final TestRestTemplate restTemplate = new TestRestTemplate();

    /**
     * The HTTP Headers to be sent on the REST call
     */
    private final HttpHeaders headers = new HttpHeaders();

    /**
     * Controller to whom SpringBoot will assign the context;
     */
    @Autowired
    private UrlShortenerController urlShortenerController;

    /**
     * Map containing short urls received from service and the
     * corresponding expected long urls
     */
    private final Map<String, String> controlTable = new HashMap();

    private final String[] inputLongUrls = new String[]{"https://google.com", "https://www.google.com,",
            "http://google.com", "http://www.google.com", "google.com", "www.google.com",
            "https://www.google.com/search?q=Grandparents%27+Day&oi=ddle&ct=119275999&hl=en-GB&sa=X&ved=0ahUKEwi8rY3qvIflAhWPRMAKHXkaDJsQPQgL&biw=1191&bih=634&dpr=1"};

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
     * POST Operation to shorten an invalid url
     *
     * @param requestPayload the input data as a {@link org.juliazo.url.shortener.model.UrlRequestPayload}
     * @return the response of the request as a {@link org.juliazo.url.shortener.model.ErrorResponsePayload}
     */
    private ResponseEntity<ErrorResponsePayload> shortenInvalidUrl(UrlRequestPayload requestPayload) {
        HttpEntity<UrlRequestPayload> entity = new HttpEntity<>(requestPayload, headers);

        return restTemplate.exchange(
                createURLWithPort("shorten"),
                HttpMethod.POST, entity, ErrorResponsePayload.class);
    }

    /**
     * GET Operation to lookup an existing short url
     *
     * @param shortUrl the short url
     * @return the response of the request as an http redirect
     */
    private ResponseEntity lookupValidShortUrl(String shortUrl) {

        return restTemplate.getForEntity(
                createURLWithPort(shortUrl),
                ResponseEntity.class);
    }

    /**
     * GET Operation to lookup a non existing short url
     *
     * @param shortUrl the short url
     * @return the response of the request as a {@link org.juliazo.url.shortener.model.ErrorResponsePayload}
     */
    private ResponseEntity<ErrorResponsePayload> lookupInvalidShortUrl(String shortUrl) {
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createURLWithPort(shortUrl),
                HttpMethod.GET, entity, ErrorResponsePayload.class);
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

    /**
     * Helper method to save the short url - long url mapping.
     *
     * @param shortUrl
     * @param longUrl
     */
    private void saveToControlTable(String shortUrl, String longUrl) {
        String absoluteUrl = makeAbsoluteUrl(longUrl);
        controlTable.putIfAbsent(shortUrl, absoluteUrl);
    }

    /**
     * Helper method to check if the url returned by the service is the expected one
     *
     * @param shortUrl
     * @param longUrl
     * @return result of comparison between actual and expected long urls
     */
    private boolean checkControlTable(String shortUrl, String longUrl) {
        String absoluteUrl = makeAbsoluteUrl(longUrl);
        String storedUrl = controlTable.get(shortUrl);

        return storedUrl.equals(absoluteUrl);
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

        saveToControlTable(shortUrl, longUrl);
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
    @ValueSource(strings = {"","https://goo gle.com"})
    public void testShortenInvalidUrl(String longUrl) {
        System.out.println("Running test for url: " + longUrl);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl);

        ResponseEntity<ErrorResponsePayload> response = shortenInvalidUrl(requestPayload);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponsePayload responsePayload = response.getBody();
        assertEquals(HttpStatus.BAD_REQUEST.value(), responsePayload.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), responsePayload.getReasonPhrase());
        assertEquals("The provided url is malformed or otherwise invalid.", responsePayload.getMessage());
    }

    /**
     * Test invalid cases for GET /lookup:
     * Shorter than 6 chars, bigger than 6 chars, non-existent.
     *
     * @param shortUrl
     */
    @ParameterizedTest
    @ValueSource(strings = {"elu39", "balling", "julia1"})
    public void testLookupInvalidUrl(String shortUrl) {
        System.out.println("Running test for url: " + shortUrl);

        ResponseEntity<ErrorResponsePayload> response = lookupInvalidShortUrl(shortUrl);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponsePayload responsePayload = response.getBody();
        assertEquals(HttpStatus.NOT_FOUND.value(), responsePayload.getStatus());

        assertEquals("The requested short url was not found.", responsePayload.getMessage());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), responsePayload.getReasonPhrase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://google.com",
            "https://www.google.com/search?q=Grandparents%27+Day&oi=ddle&ct=119275999&hl=en-GB&sa=X&ved=0ahUKEwi8rY3qvIflAhWPRMAKHXkaDJsQPQgL&biw=1191&bih=634&dpr=1"})
    public void testLookupValidUrl(String longUrl) {
        System.out.println("Running test for url: " + longUrl);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl);

        ResponseEntity<UrlResponsePayload> response = shortenValidUrl(requestPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        var shortUrl = response.getBody().getShortUrl();

        ResponseEntity actual = lookupValidShortUrl(shortUrl.substring(shortUrl.length()-6));
        assertEquals(HttpStatus.FOUND, actual.getStatusCode());
        assertEquals(makeAbsoluteUrl(longUrl), actual.getHeaders().getLocation().toString());
    }
}