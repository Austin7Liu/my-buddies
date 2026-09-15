package com.austin.module.search.domain;

import java.time.LocalDateTime;

public record SearchDocument(
        String id,
        SearchDocumentType documentType,
        Long businessId,
        String title,
        String content,
        Long topicId,
        String topicName,
        Long circleId,
        String circleName,
        Long creatorAccountId,
        String city,
        String district,
        String meetupMode,
        LocalDateTime startTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean visible) {
}
