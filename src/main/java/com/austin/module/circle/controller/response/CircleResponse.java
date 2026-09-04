package com.austin.module.circle.controller.response;

import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleStatus;
import java.time.LocalDateTime;

public record CircleResponse(Long id, Long topicId, Long creatorAccountId, String name,
        String description, String city, String district, CircleStatus status,
        String rejectionReason, Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static CircleResponse from(Circle value) {
        return new CircleResponse(value.getId(), value.getTopicId(), value.getCreatorAccountId(), value.getName(),
                value.getDescription(), value.getCity(), value.getDistrict(), value.getStatus(),
                value.getRejectionReason(), value.getVersion(), value.getCreatedAt(), value.getUpdatedAt());
    }
}

