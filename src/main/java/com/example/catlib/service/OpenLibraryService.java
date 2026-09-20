package com.example.catlib.service;

import com.example.catlib.exception.ExternalApiException;
import com.example.catlib.model.OpenLibraryBook;
import com.example.catlib.model.OpenLibraryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OpenLibraryService {

    private static final Logger logger =
            LoggerFactory.getLogger(OpenLibraryService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String openLibraryBaseUrl;

    public OpenLibraryService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            @Value("${openlibrary.base-url}") String openLibraryBaseUrl) {

        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.openLibraryBaseUrl = openLibraryBaseUrl;
    }

    public OpenLibraryBook fetchBookByTopic(String topic) throws Exception {

        URI uri = UriComponentsBuilder
                .fromUriString(openLibraryBaseUrl)
                .path("/search.json")
                .queryParam("q", topic)
                .queryParam("limit", 1)
                .build()
                .encode()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        logger.info("Sending request to Open Library: {}", uri);

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException e) {
            logger.error("Could not connect to Open Library", e);

            throw new ExternalApiException(
                    "Could not connect to Open Library",
                    502
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ExternalApiException(
                    "Open Library request was interrupted",
                    502
            );
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.error(
                    "Open Library request failed. Status: {}, topic: {}",
                    response.statusCode(),
                    topic
            );

            throw new ExternalApiException(
                    "Open Library request failed",
                    response.statusCode()
            );
        }

        OpenLibraryResponse openLibraryResponse =
                objectMapper.readValue(
                        response.body(),
                        OpenLibraryResponse.class
                );

        if (openLibraryResponse.getDocs() == null
                || openLibraryResponse.getDocs().isEmpty()) {
            return null;
        }

        return openLibraryResponse.getDocs().get(0);
    }
}