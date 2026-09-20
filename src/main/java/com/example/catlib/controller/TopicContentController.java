package com.example.catlib.controller;

import com.example.catlib.model.StorageSummaryResponse;
import com.example.catlib.model.TopicContentResponse;
import com.example.catlib.service.StorageService;
import com.example.catlib.service.TopicContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class TopicContentController {

    private final TopicContentService topicContentService;
    private final StorageService storageService;

    public TopicContentController(
            TopicContentService topicContentService,
            StorageService storageService) {

        this.topicContentService = topicContentService;
        this.storageService = storageService;
    }

    @GetMapping("/summary")
    public StorageSummaryResponse getStorageSummary() throws Exception {
        return storageService.getStoredItemsSummary();
    }

    @PostMapping("/{topic}")
    public TopicContentResponse getTopicContent(
            @PathVariable String topic) throws Exception {

        return topicContentService.fetchTopicContent(topic);
    }
}