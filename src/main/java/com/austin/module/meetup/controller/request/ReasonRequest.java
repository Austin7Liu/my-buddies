package com.austin.module.meetup.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(
        @NotBlank(message = "原因不能为空")
        @Size(max = 255, message = "原因不能超过 255 个字符")
        String reason) {
}
