package com.austin.module.post.controller.response;

import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.post.service.PostInteractionService.InteractionSummary;
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
        long likeCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PostResponse from(
            Post post,
            ProfileSummaryResponse author,
            InteractionSummary interaction) {
        return new PostResponse(post.getId(), post.getAuthorAccountId(), author, post.getTopicId(), post.getCircleId(),
                post.getContent(), post.getStatus(), post.getModerationReason(), interaction.likeCount(),
                interaction.likedByMe(), interaction.bookmarkedByMe(), post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
