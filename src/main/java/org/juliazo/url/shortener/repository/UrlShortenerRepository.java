package org.juliazo.url.shortener.repository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class UrlShortenerRepository {

    private final Map<String, URI> urlStorage = new HashMap<>();

    public URI storeOrGet(final String shortUrl, final URI validUri) {

        URI storedUrl = urlStorage.putIfAbsent(shortUrl, validUri);
        if (storedUrl == null) {
            return validUri;
        }
        return storedUrl;
    }

    public URI lookupUrl(final String shortUrl) {
        return urlStorage.get(shortUrl);
    }
}
