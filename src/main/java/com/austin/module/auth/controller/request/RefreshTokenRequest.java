package com.austin.module.auth.controller.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken) { }
