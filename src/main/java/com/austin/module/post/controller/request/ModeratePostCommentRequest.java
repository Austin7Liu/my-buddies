package com.austin.module.post.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModeratePostCommentRequest(
        @NotBlank
        @Size(max = 500)
        String reason) {
}
