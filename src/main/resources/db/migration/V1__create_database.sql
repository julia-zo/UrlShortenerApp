CREATE TABLE url_entity (
  short_url VARCHAR(6) NOT NULL UNIQUE,
  long_url VARCHAR(400) NOT NULL,
  PRIMARY KEY (long_url)
);
