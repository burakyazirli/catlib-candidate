package com.example.catlib.model;

import java.util.List;

public class StorageSummaryResponse {

    private final int total;
    private final List<StoredItemSummary> items;

    public StorageSummaryResponse(
            int total,
            List<StoredItemSummary> items) {

        this.total = total;
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public List<StoredItemSummary> getItems() {
        return items;
    }
}