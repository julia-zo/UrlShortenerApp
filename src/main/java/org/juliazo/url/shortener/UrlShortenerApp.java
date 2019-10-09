package org.juliazo.url.shortener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
class UrlShortenerApp {

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerApp.class);

    /**
     * Spring Boot application starter
     * @param args
     */
    public static void main(String[] args) {
        logger.info("Starting URL Shortener Application");
        SpringApplication.run(UrlShortenerApp.class, args);
    }

}
