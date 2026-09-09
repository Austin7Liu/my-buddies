package com.austin.module.reputation.controller.response;

import com.austin.module.reputation.service.ReputationSummary;
import java.math.BigDecimal;

public record ReputationResponse(
        long accountId,

        long offlineFulfillmentCount,

        long attendedCount,

        long absentCount,

        long excusedCount,

        BigDecimal attendanceRate,

        long receivedReviewCount,

        BigDecimal averageRating) {

    public static ReputationResponse from(ReputationSummary summary) {
        return new ReputationResponse(
                summary.accountId(),
                summary.offlineFulfillmentCount(),
                summary.attendedCount(),
                summary.absentCount(),
                summary.excusedCount(),
                summary.attendanceRate(),
                summary.receivedReviewCount(),
                summary.averageRating());
    }
}
