package com.austin.module.meetup.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.meetup.controller.request.ModerateMeetupReviewRequest;
import com.austin.module.meetup.controller.response.MeetupReviewResponse;
import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.domain.MeetupReviewStatus;
import com.austin.module.meetup.service.MeetupReviewService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequestMapping("/api/v1/admin/meetup-reviews")
public class AdminMeetupReviewController {

    private final MeetupReviewService reviewService;
    private final ProfileService profileService;

    @PatchMapping("/{reviewId}/hide")
    public ApiResponse<MeetupReviewResponse> hide(
            Authentication authentication,
            @PathVariable @Positive long reviewId,
            @Valid @RequestBody ModerateMeetupReviewRequest request) {
        return ApiResponse.success(response(reviewService.moderate(
                accountId(authentication), reviewId, MeetupReviewStatus.HIDDEN, request.reason())));
    }

    @PatchMapping("/{reviewId}/restore")
    public ApiResponse<MeetupReviewResponse> restore(
            Authentication authentication,
            @PathVariable @Positive long reviewId,
            @Valid @RequestBody ModerateMeetupReviewRequest request) {
        return ApiResponse.success(response(reviewService.moderate(
                accountId(authentication), reviewId, MeetupReviewStatus.VISIBLE, request.reason())));
    }

    private MeetupReviewResponse response(MeetupReview review) {
        var summaries = profileService.getSummaries(
                Set.of(review.getReviewerAccountId(), review.getRevieweeAccountId()).stream().toList());
        return MeetupReviewResponse.from(
                review,
                ProfileSummaryResponse.from(summaries.get(review.getReviewerAccountId())),
                ProfileSummaryResponse.from(summaries.get(review.getRevieweeAccountId())));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
