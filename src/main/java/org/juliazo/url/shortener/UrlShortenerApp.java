package org.juliazo.url.shortener;

/*
  Url Shortener Application

  This app is a REST application composed of two endpoints: a POST endpoint that receives
  an url and returns a short url generated based on an MD5 hash of the provided url with
  200 status code; a GET endpoint that receives a short url and returns a 302 status code
  redirecting the caller to the long url stored by the application, or a 404 status code
  when there is no corresponding url in the database.

  This application was developed using SpringBoot Framework and Docker.

  How to execute it:
  1: create a docker image for the service:
  on terminal run: $mvn clean install docker:build
  2: mount the service and the database containers
  on terminal run: $docker-compose up
  3: app will be available on http://localhost:80

  The database will persist with the stored data in between executions, i.e. when the
  service is stopped and started again. If you want to re-run the application in a
  clean way, you will need to stop and remove the created containers by executing the
  following command on the terminal: $docker-compose down -v

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
     *
     * @param args
     */
    public static void main(String[] args) {
        logger.info("Starting URL Shortener Application");
        SpringApplication.run(UrlShortenerApp.class, args);
    }

}
