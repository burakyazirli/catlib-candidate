package com.example.catlib.service;

import com.example.catlib.model.CatResponse;
import com.example.catlib.model.OpenLibraryBook;
import com.example.catlib.model.TopicContentResponse;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class TopicContentService {

    private final CatService catService;
    private final OpenLibraryService openLibraryService;
    private final StorageService storageService;

    public TopicContentService(
            CatService catService,
            OpenLibraryService openLibraryService,
            StorageService storageService) {

        this.catService = catService;
        this.openLibraryService = openLibraryService;
        this.storageService = storageService;
    }

    public TopicContentResponse fetchTopicContent(String topic) throws Exception {

        CatResponse cat = catService.fetchCatByTag(topic);
        OpenLibraryBook book = openLibraryService.fetchBookByTopic(topic);

        Path topicDirectory = storageService.saveImage(
                cat.getImageUrl(),
                topic
        );

        storageService.saveMetadata(
                topicDirectory,
                book
        );

        return new TopicContentResponse(
                topic,
                cat,
                book
        );
    }
}