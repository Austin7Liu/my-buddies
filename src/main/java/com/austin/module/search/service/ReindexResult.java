package com.austin.module.search.service;

import java.time.LocalDateTime;

public record ReindexResult(
        String indexName,
        int documentCount,
        LocalDateTime completedAt) {
}
