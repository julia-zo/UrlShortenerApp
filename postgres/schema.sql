CREATE DATABASE postgres;
CREATE USER app WITH PASSWORD 'app';
GRANT ALL PRIVILEGES ON DATABASE app TO app;
\connect postgres
CREATE TABLE public.urls
(
    shortUrl text NOT NULL,
    longUrl text NOT NULL
    CONSTRAINT urls_pkey PRIMARY KEY (shortUrl, longUrl),
);
ALTER TABLE public.urls
    OWNER to app;