package com.austin.module.post.controller.response;

import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record AdminPostCommentResponse(
        Long id,

        Long postId,

        Long authorAccountId,

        ProfileSummaryResponse author,

        Long parentCommentId,

        String content,

        PostCommentStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt) {

    public static AdminPostCommentResponse from(PostComment comment, ProfileSummaryResponse author) {
        return new AdminPostCommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorAccountId(),
                author,
                comment.getParentCommentId(),
                comment.getContent(),
                comment.getStatus(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
