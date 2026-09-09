package com.austin.module.reputation.mapper.model;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewStatsRow {

    private Long reviewCount;

    private BigDecimal averageRating;
}
