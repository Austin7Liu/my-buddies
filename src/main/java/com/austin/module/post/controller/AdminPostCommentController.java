package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.post.controller.request.ModeratePostCommentRequest;
import com.austin.module.post.controller.response.AdminPostCommentResponse;
import com.austin.module.post.controller.response.PostCommentResponse;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.post.service.PostCommentService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.List;
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
@RequestMapping("/api/v1/admin/post-comments")
public class AdminPostCommentController {

    private final PostCommentService commentService;
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<PageResponse<AdminPostCommentResponse>> list(
            @RequestParam(required = false) PostCommentStatus status,
            @RequestParam(required = false) @Positive Long postId,
            @RequestParam(required = false) @Positive Long authorAccountId,
            @RequestParam(defaultValue = "1") @Positive long page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) long size) {
        var comments = commentService.listForAdmin(status, postId, authorAccountId, page, size);
        var summaries = profileService.getSummaries(comments.getRecords().stream()
                .map(PostComment::getAuthorAccountId)
                .distinct()
                .toList());
        return ApiResponse.success(PageResponse.from(comments, comment -> AdminPostCommentResponse.from(
                comment, ProfileSummaryResponse.from(summaries.get(comment.getAuthorAccountId())))));
    }

    @PostMapping("/{commentId}/hide")
    public ApiResponse<PostCommentResponse> hide(
            Authentication authentication,
            @PathVariable @Positive long commentId,
            @Valid @RequestBody ModeratePostCommentRequest request) {
        return ApiResponse.success(response(
                commentService.hide(accountId(authentication), commentId, request.reason())));
    }

    @PostMapping("/{commentId}/restore")
    public ApiResponse<PostCommentResponse> restore(
            Authentication authentication,
            @PathVariable @Positive long commentId) {
        return ApiResponse.success(response(commentService.restore(accountId(authentication), commentId)));
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
