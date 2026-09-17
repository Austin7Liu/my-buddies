package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.post.controller.response.PostCommentLocationResponse;
import com.austin.module.post.service.PostCommentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/post-comments")
public class PostCommentLocationController {

    private final PostCommentService commentService;

    @GetMapping("/{commentId}/location")
    public ApiResponse<PostCommentLocationResponse> locate(
            @PathVariable @Positive long commentId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PostCommentLocationResponse.from(commentService.locate(commentId, size)));
    }
}
