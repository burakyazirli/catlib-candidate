package com.example.catlib.model;

import java.util.List;

public class OpenLibraryResponse {

    private int numFound;
    private List<OpenLibraryBook> docs;

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    public List<OpenLibraryBook> getDocs() {
        return docs;
    }

    public void setDocs(List<OpenLibraryBook> docs) {
        this.docs = docs;
    }
}