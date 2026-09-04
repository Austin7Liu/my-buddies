package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.PostModerationRequest;
import com.austin.module.post.controller.response.PostResponse;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequestMapping("/api/v1/admin/posts")
public class AdminPostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> list(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(postService.listForReview(status, page, size), PostResponse::from));
    }

    @PostMapping("/{postId}/approve")
    public ApiResponse<PostResponse> approve(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostResponse.from(postService.approve(accountId(authentication), postId)));
    }

    @PostMapping("/{postId}/reject")
    public ApiResponse<PostResponse> reject(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody PostModerationRequest request) {
        return ApiResponse.success(PostResponse.from(
                postService.reject(accountId(authentication), postId, request.reason())));
    }

    @PostMapping("/{postId}/offline")
    public ApiResponse<PostResponse> offline(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody PostModerationRequest request) {
        return ApiResponse.success(PostResponse.from(
                postService.offline(accountId(authentication), postId, request.reason())));
    }

    @PostMapping("/{postId}/restore")
    public ApiResponse<PostResponse> restore(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostResponse.from(postService.restore(accountId(authentication), postId)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
