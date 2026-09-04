package com.austin.module.post.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "帖子内容不能为空")
        @Size(max = 2000, message = "帖子内容不能超过 2000 个字符")
        String content,
        @Positive(message = "话题 ID 必须为正数")
        Long topicId,
        @Positive(message = "圈子 ID 必须为正数")
        Long circleId) {
}
