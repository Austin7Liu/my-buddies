package com.austin.module.post.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotBlank(message = "帖子内容不能为空")
        @Size(max = 2000, message = "帖子内容不能超过 2000 个字符")
        String content) {
}
