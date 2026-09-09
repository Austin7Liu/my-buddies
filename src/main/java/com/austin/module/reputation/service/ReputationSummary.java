package com.austin.module.reputation.service;

import java.math.BigDecimal;

public record ReputationSummary(
        long accountId,

        long offlineFulfillmentCount,

        long attendedCount,

        long absentCount,

        long excusedCount,

        BigDecimal attendanceRate,

        long receivedReviewCount,

        BigDecimal averageRating) {
}
