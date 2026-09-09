package com.austin.module.post.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePostCommentRequest(
        @Positive
        Long parentCommentId,

        @NotBlank
        @Size(max = 500)
        String content) {
}
