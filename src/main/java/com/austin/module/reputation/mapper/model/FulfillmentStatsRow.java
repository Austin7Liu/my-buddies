package com.austin.module.reputation.mapper.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FulfillmentStatsRow {

    private Long fulfillmentCount;

    private Long attendedCount;

    private Long absentCount;

    private Long excusedCount;
}
