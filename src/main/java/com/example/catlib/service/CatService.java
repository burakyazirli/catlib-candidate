package com.example.catlib.service;

import com.example.catlib.exception.ExternalApiException;
import com.example.catlib.model.CatResponse;
import com.fasterxml.jackson.databind.JsonNode;
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
public class CatService {

    private static final Logger logger =
            LoggerFactory.getLogger(CatService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String cataasBaseUrl;

    public CatService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            @Value("${cataas.base-url}") String cataasBaseUrl) {

        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cataasBaseUrl = cataasBaseUrl;
    }

    public CatResponse fetchCatByTag(String tag) throws Exception {

        URI uri = UriComponentsBuilder
                .fromUriString(cataasBaseUrl)
                .pathSegment("cat", tag)
                .queryParam("json", "true")
                .build()
                .encode()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        logger.info("Sending request to CATAAS: {}", uri);

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException e) {
            logger.error("Could not connect to CATAAS", e);

            throw new ExternalApiException(
                    "Could not connect to CATAAS",
                    502
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ExternalApiException(
                    "CATAAS request was interrupted",
                    502
            );
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.error(
                    "CATAAS request failed. Status: {}, tag: {}",
                    response.statusCode(),
                    tag
            );

            throw new ExternalApiException(
                    "CATAAS request failed",
                    response.statusCode()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());

        String catId = root.path("id").asText();

        if (catId.isBlank()) {
            logger.error(
                    "CATAAS response did not contain a cat id. Tag: {}",
                    tag
            );

            throw new ExternalApiException(
                    "CATAAS response did not contain a cat id",
                    502
            );
        }

        String imageUrl = cataasBaseUrl + "/cat/" + catId;

        return new CatResponse(tag, imageUrl);
    }
}