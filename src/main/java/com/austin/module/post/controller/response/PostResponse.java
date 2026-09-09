package com.austin.module.post.controller.response;

import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long authorAccountId,
        ProfileSummaryResponse author,
        Long topicId,
        Long circleId,
        String content,
        PostStatus status,
        String moderationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PostResponse from(Post post, ProfileSummaryResponse author) {
        return new PostResponse(post.getId(), post.getAuthorAccountId(), author, post.getTopicId(), post.getCircleId(),
                post.getContent(), post.getStatus(), post.getModerationReason(), post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
