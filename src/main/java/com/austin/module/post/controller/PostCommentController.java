package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.CreatePostCommentRequest;
import com.austin.module.post.controller.request.UpdatePostCommentRequest;
import com.austin.module.post.controller.response.PostCommentResponse;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.service.PostCommentService;
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
@RequestMapping("/api/v1/posts/{postId}/comments")
public class PostCommentController {

    private final PostCommentService commentService;
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<PageResponse<PostCommentResponse>> list(
            @PathVariable @Positive long postId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(commentService.list(postId, page, size)));
    }

    @PostMapping
    public ApiResponse<PostCommentResponse> create(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody CreatePostCommentRequest request) {
        return ApiResponse.success(response(commentService.create(accountId(authentication), postId,
                request.parentCommentId(), request.content())));
    }

    @PutMapping("/{commentId}")
    public ApiResponse<PostCommentResponse> update(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @PathVariable @Positive long commentId,
            @Valid @RequestBody UpdatePostCommentRequest request) {
        return ApiResponse.success(response(commentService.update(accountId(authentication), postId,
                commentId, request.content())));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<PostCommentResponse> delete(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @PathVariable @Positive long commentId) {
        return ApiResponse.success(response(
                commentService.delete(accountId(authentication), postId, commentId)));
    }

    private PageResponse<PostCommentResponse> responsePage(IPage<PostComment> page) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(PostComment::getAuthorAccountId).toList());
        return PageResponse.from(page, comment -> PostCommentResponse.from(comment,
                ProfileSummaryResponse.from(summaries.get(comment.getAuthorAccountId()))));
    }

    private PostCommentResponse response(PostComment comment) {
        var summary = profileService.getSummaries(List.of(comment.getAuthorAccountId()))
                .get(comment.getAuthorAccountId());
        return PostCommentResponse.from(comment, ProfileSummaryResponse.from(summary));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
