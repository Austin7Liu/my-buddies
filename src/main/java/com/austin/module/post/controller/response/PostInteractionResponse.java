package com.austin.module.post.controller.response;

import com.austin.module.post.service.PostInteractionService.InteractionSummary;

public record PostInteractionResponse(
        long likeCount,
        boolean likedByMe,
        boolean bookmarkedByMe) {

    public static PostInteractionResponse from(InteractionSummary summary) {
        return new PostInteractionResponse(
                summary.likeCount(),
                summary.likedByMe(),
                summary.bookmarkedByMe());
    }
}
