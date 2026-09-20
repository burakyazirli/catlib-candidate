package com.example.catlib.model;

public class TopicContentResponse {

    private final String topic;
    private final CatResponse cat;
    private final OpenLibraryBook book;

    public TopicContentResponse(
            String topic,
            CatResponse cat,
            OpenLibraryBook book) {

        this.topic = topic;
        this.cat = cat;
        this.book = book;
    }

    public String getTopic() {
        return topic;
    }

    public CatResponse getCat() {
        return cat;
    }

    public OpenLibraryBook getBook() {
        return book;
    }
}