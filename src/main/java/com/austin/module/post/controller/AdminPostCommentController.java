package com.austin.module.post.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.post.controller.request.ModeratePostCommentRequest;
import com.austin.module.post.controller.response.PostCommentResponse;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.service.PostCommentService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequestMapping("/api/v1/admin/post-comments")
public class AdminPostCommentController {

    private final PostCommentService commentService;
    private final ProfileService profileService;

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
