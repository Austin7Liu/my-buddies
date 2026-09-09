package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.domain.MeetupReviewStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record MeetupReviewResponse(
        Long id,

        Long meetupId,

        ProfileSummaryResponse reviewer,

        ProfileSummaryResponse reviewee,

        Integer rating,

        String comment,

        MeetupReviewStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt) {

    public static MeetupReviewResponse from(
            MeetupReview review,
            ProfileSummaryResponse reviewer,
            ProfileSummaryResponse reviewee) {
        return new MeetupReviewResponse(
                review.getId(),
                review.getMeetupId(),
                reviewer,
                reviewee,
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
