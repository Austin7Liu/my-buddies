package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.CreatePostRequest;
import com.austin.module.post.controller.request.UpdatePostRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> listPublic(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(postService.listPublic(page, size), PostResponse::from));
    }

    @GetMapping("/topics/{topicId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByTopic(
            @PathVariable @Positive long topicId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                postService.listByTopic(topicId, page, size), PostResponse::from));
    }

    @GetMapping("/circles/{circleId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByCircle(
            @PathVariable @Positive long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                postService.listByCircle(circleId, page, size), PostResponse::from));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostResponse> getPublic(@PathVariable @Positive long postId) {
        return ApiResponse.success(PostResponse.from(postService.getPublic(postId)));
    }

    @GetMapping("/posts/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<PostResponse>> listMine(
            Authentication authentication,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                postService.listMine(accountId(authentication), status, page, size), PostResponse::from));
    }

    @PostMapping("/posts")
    public ApiResponse<PostResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(PostResponse.from(postService.create(accountId(authentication), request.content(),
                request.topicId(), request.circleId())));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<PostResponse> update(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(PostResponse.from(
                postService.update(accountId(authentication), postId, request.content())));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<PostResponse> delete(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostResponse.from(postService.delete(accountId(authentication), postId)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
