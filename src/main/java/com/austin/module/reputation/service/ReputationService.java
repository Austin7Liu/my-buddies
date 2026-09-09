package com.austin.module.reputation.service;

import com.austin.module.profile.service.ProfileService;
import com.austin.module.reputation.mapper.ReputationMapper;
import com.austin.module.reputation.mapper.model.FulfillmentStatsRow;
import com.austin.module.reputation.mapper.model.ReviewStatsRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReputationService {

    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private final ReputationMapper reputationMapper;
    private final ProfileService profileService;

    @Transactional(readOnly = true)
    public ReputationSummary getPublic(long accountId) {
        profileService.getPublic(accountId);
        FulfillmentStatsRow fulfillment = reputationMapper.selectFulfillmentStats(accountId);
        ReviewStatsRow review = reputationMapper.selectReviewStats(accountId);
        long attended = fulfillment.getAttendedCount();
        long absent = fulfillment.getAbsentCount();
        return new ReputationSummary(
                accountId,
                fulfillment.getFulfillmentCount(),
                attended,
                absent,
                fulfillment.getExcusedCount(),
                attendanceRate(attended, absent),
                review.getReviewCount(),
                scaled(review.getAverageRating()));
    }

    private BigDecimal attendanceRate(long attended, long absent) {
        long counted = attended + absent;
        if (counted == 0) {
            return null;
        }
        return BigDecimal.valueOf(attended)
                .multiply(PERCENT)
                .divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaled(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
