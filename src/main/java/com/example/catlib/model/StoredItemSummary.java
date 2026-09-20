package com.example.catlib.model;

public class StoredItemSummary {

    private final String folderName;
    private final boolean imageStored;
    private final boolean metadataStored;
    private final OpenLibraryBook book;

    public StoredItemSummary(
            String folderName,
            boolean imageStored,
            boolean metadataStored,
            OpenLibraryBook book) {

        this.folderName = folderName;
        this.imageStored = imageStored;
        this.metadataStored = metadataStored;
        this.book = book;
    }

    public String getFolderName() {
        return folderName;
    }

    public boolean isImageStored() {
        return imageStored;
    }

    public boolean isMetadataStored() {
        return metadataStored;
    }

    public OpenLibraryBook getBook() {
        return book;
    }
}