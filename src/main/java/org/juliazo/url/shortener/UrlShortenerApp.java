package org.juliazo.url.shortener;

/*
  Url Shortener Application
  This app is a REST application composed of two endpoints: a POST endpoint that receives
  an url and returns a short url generated based on a MD5 hash on the provided url; a GET
  endpoint that receives a short url and returns a 302 status code redirecting the caller
  to the long url stored by the application, or 404 in case there is no corresponding url
  in the database.

  This application was developed using SpringBoot Framework and Docker.

  How to execute it:
  run mvn clean install docker:build
  run docker-compose up
  app will be available on http://localhost:80

  @author Julia Zottis
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UrlShortenerApp {

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
