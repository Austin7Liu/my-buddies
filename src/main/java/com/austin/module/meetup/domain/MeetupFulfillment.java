package com.austin.module.meetup.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("meetup_fulfillment")
public class MeetupFulfillment {

    @TableId
    private Long id;

    private Long meetupId;

    private Long accountId;

    private FulfillmentResult result;

    private FulfillmentSource source;

    private LocalDateTime settledAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long adjustedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String adjustmentReason;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
