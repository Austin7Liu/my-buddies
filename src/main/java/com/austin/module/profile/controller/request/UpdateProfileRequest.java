package com.austin.module.profile.controller.request;

import com.austin.module.profile.domain.AvatarCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 30, message = "昵称长度应为 2 到 30 个字符")
        String nickname,
        @NotNull(message = "头像不能为空")
        AvatarCode avatarCode,
        @Size(max = 200, message = "个人简介不能超过 200 个字符")
        String bio,
        @Size(max = 64, message = "城市不能超过 64 个字符")
        String city,
        @Size(max = 64, message = "区域不能超过 64 个字符")
        String district) {
}
