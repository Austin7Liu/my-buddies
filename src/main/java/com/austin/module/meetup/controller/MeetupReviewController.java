package com.austin.module.meetup.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.meetup.controller.request.CreateMeetupReviewRequest;
import com.austin.module.meetup.controller.request.UpdateMeetupReviewRequest;
import com.austin.module.meetup.controller.response.MeetupReviewResponse;
import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.service.MeetupReviewService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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
public class MeetupReviewController {

    private final MeetupReviewService reviewService;
    private final ProfileService profileService;

    @PostMapping("/meetups/{meetupId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MeetupReviewResponse> create(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody CreateMeetupReviewRequest request) {
        return ApiResponse.success(response(reviewService.create(
                accountId(authentication), meetupId, request.revieweeAccountId(),
                request.rating(), request.comment())));
    }

    @PutMapping("/meetups/{meetupId}/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MeetupReviewResponse> update(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @PathVariable @Positive long reviewId,
            @Valid @RequestBody UpdateMeetupReviewRequest request) {
        return ApiResponse.success(response(reviewService.update(
                accountId(authentication), meetupId, reviewId, request.rating(), request.comment())));
    }

    @GetMapping("/meetups/{meetupId}/reviews/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<MeetupReviewResponse>> listMine(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(
                reviewService.listMine(accountId(authentication), meetupId, page, size)));
    }

    @GetMapping("/profiles/{accountId}/reviews")
    public ApiResponse<PageResponse<MeetupReviewResponse>> listReceived(
            @PathVariable @Positive long accountId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        profileService.getPublic(accountId);
        return ApiResponse.success(responsePage(reviewService.listReceived(accountId, page, size)));
    }

    private PageResponse<MeetupReviewResponse> responsePage(IPage<MeetupReview> page) {
        Set<Long> accountIds = new LinkedHashSet<>();
        page.getRecords().forEach(review -> {
            accountIds.add(review.getReviewerAccountId());
            accountIds.add(review.getRevieweeAccountId());
        });
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(accountIds.stream().toList());
        return PageResponse.from(page, review -> response(review, summaries));
    }

    private MeetupReviewResponse response(MeetupReview review) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                Set.of(review.getReviewerAccountId(), review.getRevieweeAccountId()).stream().toList());
        return response(review, summaries);
    }

    private MeetupReviewResponse response(
            MeetupReview review,
            Map<Long, ProfileService.ProfileSummary> summaries) {
        return MeetupReviewResponse.from(
                review,
                ProfileSummaryResponse.from(summaries.get(review.getReviewerAccountId())),
                ProfileSummaryResponse.from(summaries.get(review.getRevieweeAccountId())));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
