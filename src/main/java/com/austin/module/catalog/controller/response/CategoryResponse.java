package com.austin.module.catalog.controller.response;

import com.austin.module.catalog.domain.Category;
import java.time.LocalDateTime;

public record CategoryResponse(Long id, String code, String name, String description,
        Integer sortOrder, Boolean enabled, Integer version,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static CategoryResponse from(Category value) {
        return new CategoryResponse(value.getId(), value.getCode(), value.getName(),
                value.getDescription(), value.getSortOrder(), value.getEnabled(), value.getVersion(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}

