package com.austin.module.reputation.mapper;

import com.austin.module.reputation.mapper.model.FulfillmentStatsRow;
import com.austin.module.reputation.mapper.model.ReviewStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReputationMapper {

    FulfillmentStatsRow selectFulfillmentStats(@Param("accountId") long accountId);

    ReviewStatsRow selectReviewStats(@Param("accountId") long accountId);
}
