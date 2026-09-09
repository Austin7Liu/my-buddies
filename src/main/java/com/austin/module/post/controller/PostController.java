package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.CreatePostRequest;
import com.austin.module.post.controller.request.UpdatePostRequest;
import com.austin.module.post.controller.response.PostResponse;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.service.PostService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
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
    private final ProfileService profileService;

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> listPublic(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(postService.listPublic(page, size)));
    }

    @GetMapping("/topics/{topicId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByTopic(
            @PathVariable @Positive long topicId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(postService.listByTopic(topicId, page, size)));
    }

    @GetMapping("/circles/{circleId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByCircle(
            @PathVariable @Positive long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(postService.listByCircle(circleId, page, size)));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostResponse> getPublic(@PathVariable @Positive long postId) {
        return ApiResponse.success(response(postService.getPublic(postId)));
    }

    @GetMapping("/posts/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<PostResponse>> listMine(
            Authentication authentication,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(
                postService.listMine(accountId(authentication), status, page, size)));
    }

    @PostMapping("/posts")
    public ApiResponse<PostResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(response(postService.create(accountId(authentication), request.content(),
                request.topicId(), request.circleId())));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<PostResponse> update(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(response(
                postService.update(accountId(authentication), postId, request.content())));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<PostResponse> delete(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(response(postService.delete(accountId(authentication), postId)));
    }

    private PageResponse<PostResponse> responsePage(IPage<Post> page) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(Post::getAuthorAccountId).toList());
        return PageResponse.from(page, post -> PostResponse.from(post,
                ProfileSummaryResponse.from(summaries.get(post.getAuthorAccountId()))));
    }

    private PostResponse response(Post post) {
        var summary = profileService.getSummaries(List.of(post.getAuthorAccountId()))
                .get(post.getAuthorAccountId());
        return PostResponse.from(post, ProfileSummaryResponse.from(summary));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
