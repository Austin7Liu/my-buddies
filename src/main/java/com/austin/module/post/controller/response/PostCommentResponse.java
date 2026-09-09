package com.austin.module.post.controller.response;

import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record PostCommentResponse(
        Long id,
        Long postId,
        ProfileSummaryResponse author,
        Long parentCommentId,
        String content,
        PostCommentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PostCommentResponse from(PostComment comment, ProfileSummaryResponse author) {
        String visibleContent = comment.getStatus() == PostCommentStatus.VISIBLE
                ? comment.getContent() : null;
        return new PostCommentResponse(comment.getId(), comment.getPostId(), author,
                comment.getParentCommentId(), visibleContent, comment.getStatus(),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
