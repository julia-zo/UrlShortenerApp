package org.juliazo.url.shortener;

import org.juliazo.url.shortener.controller.UrlShortenerController;
import org.juliazo.url.shortener.model.ErrorResponsePayload;
import org.juliazo.url.shortener.model.UrlRequestPayload;
import org.juliazo.url.shortener.model.UrlResponsePayload;
import org.juliazo.url.shortener.repository.UrlShortenerRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Url Shortener Application
 * To run only integration tests use command line:
 * mvn clean test -Dtest=UrlShortenerAppIntegrationTest
 * <p>
 * To run all tests except this one (only unit tests)
 * use command line:
 * mvn clean test -Dtest=!UrlShortenerAppIntegrationTest
 * <p>
 * Coverage report will be in ./target/jacoco-coverage/index.html
 */
@RunWith(JUnitPlatform.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = UrlShortenerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UrlShortenerAppIntegrationTest {

    public static final int SHORT_URL_SIZE = 6;
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerApp.class);

    static GenericContainer postgres = new PostgreSQLContainer("postgres:9");

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private final HttpHeaders headers = new HttpHeaders();

    @BeforeAll
    static void setup() {
        postgres.start();
    }

    @AfterAll
    static void tearDown() {
        postgres.stop();
    }

    @Autowired
    private UrlShortenerController urlShortenerController;

    @Autowired
    private UrlShortenerRepository urlShortenerRepository;

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
        var randomString = RandomStringUtils.randomAlphabetic(10);
        logger.debug("Running test for url: " + longUrl + randomString);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl + randomString);

        ResponseEntity<UrlResponsePayload> response = shortenValidUrl(requestPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UrlResponsePayload responsePayload = response.getBody();
        assertNotNull(responsePayload);
        assertEquals(longUrl + randomString, responsePayload.getLongUrl());
        var shortUrl = responsePayload.getShortUrl();
        assertNotNull(shortUrl);
        assertFalse(shortUrl.isEmpty());
    }

    /**
     * Test POST /shorten for the same url
     * with or without http protocol = same.
     */
    @Test
    public void testShortenTheSameValidUrl() {
        var randomString = RandomStringUtils.randomAlphabetic(10);
        var longUrl = "http://google.com" + randomString;
        UrlRequestPayload requestPayload1 = new UrlRequestPayload();
        requestPayload1.setLongUrl(longUrl);
        UrlRequestPayload requestPayload2 = new UrlRequestPayload();
        requestPayload2.setLongUrl("google.com" + randomString);

        ResponseEntity<UrlResponsePayload> response1 = shortenValidUrl(requestPayload1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        ResponseEntity<UrlResponsePayload> response2 = shortenValidUrl(requestPayload2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        UrlResponsePayload responsePayload1 = response1.getBody();
        UrlResponsePayload responsePayload2 = response2.getBody();
        assertNotNull(responsePayload1);
        assertNotNull(responsePayload2);
        var shortUrl1 = responsePayload1.getShortUrl();
        var shortUrl2 = responsePayload2.getShortUrl();

        assertShortUrlOk(shortUrl1, shortUrl2);
        assertEquals(shortUrl1, shortUrl2);
    }

    /**
     * Test POST /shorten for the same url
     * http and https protocols = different
     * with or without www = different
     */
    @Test
    public void testShortenDifferentValidUrl() {
        var randomString = RandomStringUtils.randomAlphabetic(10);
        var longUrl = "http://google.com" + randomString;
        UrlRequestPayload requestPayload1 = new UrlRequestPayload();
        requestPayload1.setLongUrl(longUrl);
        UrlRequestPayload requestPayload2 = new UrlRequestPayload();
        requestPayload2.setLongUrl("http://www.google.com" + randomString);

        ResponseEntity<UrlResponsePayload> response1 = shortenValidUrl(requestPayload1);
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        ResponseEntity<UrlResponsePayload> response2 = shortenValidUrl(requestPayload2);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        UrlResponsePayload responsePayload1 = response1.getBody();
        UrlResponsePayload responsePayload2 = response2.getBody();
        assertNotNull(responsePayload1);
        assertNotNull(responsePayload2);
        var shortUrl1 = responsePayload1.getShortUrl();
        var shortUrl2 = responsePayload2.getShortUrl();

        assertShortUrlOk(shortUrl1, shortUrl2);
        assertNotEquals(shortUrl1, shortUrl2);
    }

    private void assertShortUrlOk(String shortUrl1, String shortUrl2) {
        assertNotNull(shortUrl1);
        assertFalse(shortUrl1.isEmpty());
        assertNotNull(shortUrl2);
        assertFalse(shortUrl2.isEmpty());
    }

    /**
     * Test invalid cases for POST /shorten:
     * Null, empty string, invalid character.
     *
     * @param longUrl
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "https://goo gle.com", "http://goog|e.com"})
    public void testShortenInvalidUrl(String longUrl) {
        logger.debug("Running test for url: " + longUrl);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl);

        ResponseEntity<ErrorResponsePayload> response = restTemplate.exchange(
                createURLWithPort("shorten"), HttpMethod.POST,
                new HttpEntity<>(requestPayload, headers), ErrorResponsePayload.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponsePayload responsePayload = response.getBody();
        assertNotNull(responsePayload);
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
        logger.debug("Running test for url: " + shortUrl);

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
        var randomString = RandomStringUtils.randomAlphabetic(10);
        logger.debug("Running test for url: " + longUrl + randomString);
        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl(longUrl + randomString);

        ResponseEntity<UrlResponsePayload> response = shortenValidUrl(requestPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        var shortUrl = response.getBody().getShortUrl();
        shortUrl = shortUrl.substring(shortUrl.length() - SHORT_URL_SIZE);

        ResponseEntity actual = restTemplate.getForEntity(
                createURLWithPort(shortUrl),
                ResponseEntity.class);

        assertEquals(HttpStatus.FOUND, actual.getStatusCode());
        assertEquals(makeAbsoluteUrl(longUrl + randomString), actual.getHeaders().getLocation().toString());
    }

    @Test
    public void testConcurrentShortenRequestsWithSameUrl() {
        final int numOfConcurrentRequests = 3;
        AtomicInteger successfulRuns = new AtomicInteger(0);

        UrlRequestPayload requestPayload = new UrlRequestPayload();
        requestPayload.setLongUrl("http://" + RandomStringUtils.randomAlphabetic(10) + ".com");

        CountDownLatch requestsLatch = new CountDownLatch(numOfConcurrentRequests);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        for (int i = 0; i < numOfConcurrentRequests; i++) {
            new Thread(() -> {
                try {
                    releaseLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    ResponseEntity<UrlResponsePayload> response = shortenValidUrl(requestPayload);
                    if (HttpStatus.OK.equals(response.getStatusCode())) {
                        successfulRuns.incrementAndGet();
                    }
                } finally {
                    requestsLatch.countDown();
                }
            }).start();
        }

        releaseLatch.countDown();
        try {
            requestsLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(3, successfulRuns.get());
    }
}