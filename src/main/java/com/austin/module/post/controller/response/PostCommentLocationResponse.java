package com.austin.module.post.controller.response;

import com.austin.module.post.service.PostCommentService;

public record PostCommentLocationResponse(
        Long postId,

        Long commentId,

        long page) {

    public static PostCommentLocationResponse from(PostCommentService.CommentLocation location) {
        return new PostCommentLocationResponse(location.postId(), location.commentId(), location.page());
    }
}
