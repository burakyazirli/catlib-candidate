package com.example.catlib.model;

public class CatResponse {

    private String tag;
    private String imageUrl;

    public CatResponse() {}

    public CatResponse(String tag, String imageUrl) {
        this.tag = tag;
        this.imageUrl = imageUrl;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
