package com.example.catlib.service;

import com.example.catlib.exception.ExternalApiException;
import com.example.catlib.model.OpenLibraryBook;
import com.example.catlib.model.StoredItemSummary;
import com.example.catlib.model.StorageSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class StorageService {

    private final Path storageDir;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public StorageService(
            @Value("${app.storage-dir}") String storageDir,
            ObjectMapper objectMapper,
            HttpClient httpClient) {

        this.storageDir = Path.of(storageDir);
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public Path saveImage(String imageUrl, String topic) throws Exception {

        String safeTopic = topic.replaceAll("[^a-zA-Z0-9-_]", "_");

        Path topicDirectory = storageDir.resolve(
                safeTopic + "-" + System.currentTimeMillis()
        );

        Files.createDirectories(topicDirectory);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
        } catch (IOException e) {
            throw new ExternalApiException(
                    "Could not download cat image",
                    502
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ExternalApiException(
                    "Cat image download was interrupted",
                    502
            );
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalApiException(
                    "Could not download cat image",
                    response.statusCode()
            );
        }

        Path imagePath = topicDirectory.resolve("cat.jpg");

        Files.write(imagePath, response.body());

        return topicDirectory;
    }

    public void saveMetadata(
            Path topicDirectory,
            OpenLibraryBook book) throws Exception {

        Path metadataPath = topicDirectory.resolve("metadata.json");

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(metadataPath.toFile(), book);
    }

    public StorageSummaryResponse getStoredItemsSummary() throws Exception {

        List<StoredItemSummary> items = new ArrayList<>();

        if (!Files.exists(storageDir)) {
            return new StorageSummaryResponse(0, items);
        }

        try (DirectoryStream<Path> directories =
                Files.newDirectoryStream(storageDir)) {

            for (Path directory : directories) {

                if (!Files.isDirectory(directory)) {
                    continue;
                }

                Path imagePath = directory.resolve("cat.jpg");
                Path metadataPath = directory.resolve("metadata.json");

                boolean imageStored = Files.exists(imagePath);
                boolean metadataStored = Files.exists(metadataPath);

                OpenLibraryBook book = null;

                if (metadataStored) {
                    book = objectMapper.readValue(
                            metadataPath.toFile(),
                            OpenLibraryBook.class
                    );
                }

                StoredItemSummary item = new StoredItemSummary(
                        directory.getFileName().toString(),
                        imageStored,
                        metadataStored,
                        book
                );

                items.add(item);
            }
        }

        return new StorageSummaryResponse(
                items.size(),
                items
        );
    }
}