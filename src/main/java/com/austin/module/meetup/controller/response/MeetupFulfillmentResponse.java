package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.FulfillmentResult;
import com.austin.module.meetup.domain.FulfillmentSource;
import com.austin.module.meetup.domain.MeetupFulfillment;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record MeetupFulfillmentResponse(
        Long id,

        Long meetupId,

        Long accountId,

        ProfileSummaryResponse profile,

        FulfillmentResult result,

        FulfillmentSource source,

        LocalDateTime settledAt,

        Long adjustedBy,

        String adjustmentReason,

        LocalDateTime updatedAt) {

    public static MeetupFulfillmentResponse from(
            MeetupFulfillment fulfillment,
            ProfileSummaryResponse profile) {
        return new MeetupFulfillmentResponse(
                fulfillment.getId(),
                fulfillment.getMeetupId(),
                fulfillment.getAccountId(),
                profile,
                fulfillment.getResult(),
                fulfillment.getSource(),
                fulfillment.getSettledAt(),
                fulfillment.getAdjustedBy(),
                fulfillment.getAdjustmentReason(),
                fulfillment.getUpdatedAt());
    }
}
