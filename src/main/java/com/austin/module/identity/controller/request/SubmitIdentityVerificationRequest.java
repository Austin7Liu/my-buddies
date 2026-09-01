package com.austin.module.identity.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitIdentityVerificationRequest(
        @NotBlank(message = "真实姓名不能为空")
        @Size(min = 2, max = 50, message = "真实姓名长度应为 2 到 50 个字符")
        String realName,
        @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "[1-9]\\d{16}[0-9Xx]", message = "身份证号格式不正确")
        String identityNumber) {
}

