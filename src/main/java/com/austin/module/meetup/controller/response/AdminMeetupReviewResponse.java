package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.domain.MeetupReviewStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record AdminMeetupReviewResponse(
        Long id,

        Long meetupId,

        Long reviewerAccountId,

        ProfileSummaryResponse reviewer,

        Long revieweeAccountId,

        ProfileSummaryResponse reviewee,

        Integer rating,

        String comment,

        MeetupReviewStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt) {

    public static AdminMeetupReviewResponse from(
            MeetupReview review,
            ProfileSummaryResponse reviewer,
            ProfileSummaryResponse reviewee) {
        return new AdminMeetupReviewResponse(
                review.getId(),
                review.getMeetupId(),
                review.getReviewerAccountId(),
                reviewer,
                review.getRevieweeAccountId(),
                reviewee,
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
