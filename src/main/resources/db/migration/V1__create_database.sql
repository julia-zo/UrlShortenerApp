CREATE TABLE url_entity (
  short_url VARCHAR(6) NOT NULL,
  long_url VARCHAR(400) NOT NULL,
  PRIMARY KEY (short_url, long_url)
);
