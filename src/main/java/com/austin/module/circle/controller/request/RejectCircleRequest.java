package com.austin.module.circle.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCircleRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 255, message = "驳回原因不能超过 255 个字符")
        String reason) { }
