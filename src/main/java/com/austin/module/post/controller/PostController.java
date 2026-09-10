package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.CreatePostRequest;
import com.austin.module.post.controller.request.UpdatePostRequest;
import com.austin.module.post.controller.response.PostResponse;
import com.austin.module.post.controller.response.PostInteractionResponse;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.service.PostService;
import com.austin.module.post.service.PostInteractionService;
import com.austin.module.post.service.PostInteractionService.InteractionSummary;
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
    private final PostInteractionService interactionService;
    private final ProfileService profileService;

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostResponse>> listPublic(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(postService.listPublic(page, size), optionalAccountId(authentication)));
    }

    @GetMapping("/topics/{topicId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByTopic(
            Authentication authentication,
            @PathVariable @Positive long topicId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(
                postService.listByTopic(topicId, page, size), optionalAccountId(authentication)));
    }

    @GetMapping("/circles/{circleId}/posts")
    public ApiResponse<PageResponse<PostResponse>> listByCircle(
            Authentication authentication,
            @PathVariable @Positive long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(
                postService.listByCircle(circleId, page, size), optionalAccountId(authentication)));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostResponse> getPublic(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(response(postService.getPublic(postId), optionalAccountId(authentication)));
    }

    @GetMapping("/posts/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<PostResponse>> listMine(
            Authentication authentication,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        long accountId = accountId(authentication);
        return ApiResponse.success(responsePage(
                postService.listMine(accountId, status, page, size), accountId));
    }

    @PostMapping("/posts")
    public ApiResponse<PostResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        long accountId = accountId(authentication);
        return ApiResponse.success(response(postService.create(accountId, request.content(),
                request.topicId(), request.circleId()), accountId));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<PostResponse> update(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        long accountId = accountId(authentication);
        return ApiResponse.success(response(
                postService.update(accountId, postId, request.content()), accountId));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<PostResponse> delete(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        long accountId = accountId(authentication);
        return ApiResponse.success(response(postService.delete(accountId, postId), accountId));
    }

    @PutMapping("/posts/{postId}/like")
    public ApiResponse<PostInteractionResponse> like(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostInteractionResponse.from(
                interactionService.like(accountId(authentication), postId)));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<PostInteractionResponse> unlike(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostInteractionResponse.from(
                interactionService.unlike(accountId(authentication), postId)));
    }

    @PutMapping("/posts/{postId}/bookmark")
    public ApiResponse<PostInteractionResponse> bookmark(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostInteractionResponse.from(
                interactionService.bookmark(accountId(authentication), postId)));
    }

    @DeleteMapping("/posts/{postId}/bookmark")
    public ApiResponse<PostInteractionResponse> removeBookmark(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(PostInteractionResponse.from(
                interactionService.removeBookmark(accountId(authentication), postId)));
    }

    @GetMapping("/me/bookmarked-posts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<PostResponse>> listBookmarkedPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        long accountId = accountId(authentication);
        return ApiResponse.success(responsePage(
                interactionService.listBookmarkedPosts(accountId, page, size), accountId));
    }

    @GetMapping("/me/post-feed")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<PostResponse>> listPersonalFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        long accountId = accountId(authentication);
        return ApiResponse.success(responsePage(postService.listPersonalFeed(accountId, page, size), accountId));
    }

    private PageResponse<PostResponse> responsePage(IPage<Post> page, Long accountId) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(Post::getAuthorAccountId).toList());
        Map<Long, InteractionSummary> interactions = interactionService.summarize(
                page.getRecords().stream().map(Post::getId).toList(), accountId);
        return PageResponse.from(page, post -> PostResponse.from(post,
                ProfileSummaryResponse.from(summaries.get(post.getAuthorAccountId())), interactions.get(post.getId())));
    }

    private PostResponse response(Post post, Long accountId) {
        var summary = profileService.getSummaries(List.of(post.getAuthorAccountId()))
                .get(post.getAuthorAccountId());
        InteractionSummary interaction = interactionService.summarize(List.of(post.getId()), accountId)
                .get(post.getId());
        return PostResponse.from(post, ProfileSummaryResponse.from(summary), interaction);
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    private Long optionalAccountId(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())
                ? null
                : accountId(authentication);
    }
}
