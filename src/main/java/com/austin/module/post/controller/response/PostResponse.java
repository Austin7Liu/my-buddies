package com.austin.module.post.controller.response;

import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long authorAccountId,
        Long topicId,
        Long circleId,
        String content,
        PostStatus status,
        String moderationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getAuthorAccountId(), post.getTopicId(), post.getCircleId(),
                post.getContent(), post.getStatus(), post.getModerationReason(), post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
