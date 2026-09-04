package com.austin.module.post.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostModerationRequest(
        @NotBlank(message = "处理原因不能为空")
        @Size(max = 255, message = "处理原因不能超过 255 个字符")
        String reason) {
}
