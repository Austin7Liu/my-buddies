package com.austin.module.catalog.controller.response;

import com.austin.module.catalog.domain.Topic;
import java.time.LocalDateTime;

public record TopicResponse(
        Long id,
        Long categoryId,
        String code,
        String name,
        String description,
        Integer sortOrder,
        Boolean enabled,
        boolean followedByMe,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TopicResponse from(Topic value) {
        return from(value, false);
    }

    public static TopicResponse from(Topic value, boolean followedByMe) {
        return new TopicResponse(value.getId(), value.getCategoryId(), value.getCode(), value.getName(),
                value.getDescription(), value.getSortOrder(), value.getEnabled(), followedByMe, value.getVersion(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
