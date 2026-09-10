package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.PostModerationRequest;
import com.austin.module.post.controller.response.PostResponse;
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
    private final PostInteractionService interactionService;
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> list(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(postService.listForReview(status, page, size)));
    }

    @PostMapping("/{postId}/approve")
    public ApiResponse<PostResponse> approve(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(response(postService.approve(accountId(authentication), postId)));
    }

    @PostMapping("/{postId}/reject")
    public ApiResponse<PostResponse> reject(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody PostModerationRequest request) {
        return ApiResponse.success(response(
                postService.reject(accountId(authentication), postId, request.reason())));
    }

    @PostMapping("/{postId}/offline")
    public ApiResponse<PostResponse> offline(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody PostModerationRequest request) {
        return ApiResponse.success(response(
                postService.offline(accountId(authentication), postId, request.reason())));
    }

    @PostMapping("/{postId}/restore")
    public ApiResponse<PostResponse> restore(
            Authentication authentication,
            @PathVariable @Positive long postId) {
        return ApiResponse.success(response(postService.restore(accountId(authentication), postId)));
    }

    private PageResponse<PostResponse> responsePage(IPage<Post> page) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(Post::getAuthorAccountId).toList());
        Map<Long, InteractionSummary> interactions = interactionService.summarize(
                page.getRecords().stream().map(Post::getId).toList(), null);
        return PageResponse.from(page, post -> PostResponse.from(post,
                ProfileSummaryResponse.from(summaries.get(post.getAuthorAccountId())), interactions.get(post.getId())));
    }

    private PostResponse response(Post post) {
        var summary = profileService.getSummaries(List.of(post.getAuthorAccountId()))
                .get(post.getAuthorAccountId());
        InteractionSummary interaction = interactionService.summarize(List.of(post.getId()), null).get(post.getId());
        return PostResponse.from(post, ProfileSummaryResponse.from(summary), interaction);
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
