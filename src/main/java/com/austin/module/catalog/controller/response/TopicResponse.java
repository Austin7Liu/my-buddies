package com.austin.module.catalog.controller.response;

import com.austin.module.catalog.domain.Topic;
import java.time.LocalDateTime;

public record TopicResponse(Long id, Long categoryId, String code, String name, String description,
        Integer sortOrder, Boolean enabled, Integer version,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static TopicResponse from(Topic value) {
        return new TopicResponse(value.getId(), value.getCategoryId(), value.getCode(), value.getName(),
                value.getDescription(), value.getSortOrder(), value.getEnabled(), value.getVersion(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}

