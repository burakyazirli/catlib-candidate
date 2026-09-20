package com.example.catlib.service;

import com.example.catlib.model.CatResponse;
import com.example.catlib.model.OpenLibraryBook;
import com.example.catlib.model.TopicContentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicContentServiceTest {

    @Mock
    private CatService catService;

    @Mock
    private OpenLibraryService openLibraryService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private TopicContentService topicContentService;

    @Test
    void shouldFetchAndStoreTopicContent() throws Exception {

        CatResponse cat =
                new CatResponse(
                        "space",
                        "https://cataas.com/cat/test-id"
                );

        OpenLibraryBook book = new OpenLibraryBook();
        book.setKey("/works/test");
        book.setTitle("Test Book");
        book.setAuthorNames(List.of("Test Author"));
        book.setFirstPublishYear(2020);

        Path directory = Path.of("downloads/space-test");

        when(catService.fetchCatByTag("space"))
                .thenReturn(cat);

        when(openLibraryService.fetchBookByTopic("space"))
                .thenReturn(book);

        when(storageService.saveImage(
                cat.getImageUrl(),
                "space"
        )).thenReturn(directory);

        TopicContentResponse result =
                topicContentService.fetchTopicContent("space");

        assertEquals("space", result.getTopic());
        assertEquals(cat, result.getCat());
        assertEquals(book, result.getBook());

        verify(storageService).saveImage(
                cat.getImageUrl(),
                "space"
        );

        verify(storageService).saveMetadata(
                directory,
                book
        );
    }
}