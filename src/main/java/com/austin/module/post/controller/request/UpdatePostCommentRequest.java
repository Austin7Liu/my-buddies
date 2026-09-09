package com.austin.module.post.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostCommentRequest(
        @NotBlank
        @Size(max = 500)
        String content) {
}
