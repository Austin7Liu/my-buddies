package com.austin.module.circle.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCircleRequest(
        @NotBlank
        @Size(max = 64, message = "圈子名称不能超过 64 个字符")
        String name,
        @Size(max = 500, message = "圈子描述不能超过 500 个字符")
        String description,
        @NotBlank
        @Size(max = 64, message = "城市不能超过 64 个字符")
        String city,
        @Size(max = 64, message = "区域不能超过 64 个字符")
        String district) { }
