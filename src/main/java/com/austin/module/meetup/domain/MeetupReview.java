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
@TableName("meetup_review")
public class MeetupReview {

    @TableId
    private Long id;

    private Long meetupId;

    private Long reviewerAccountId;

    private Long revieweeAccountId;

    private Integer rating;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String comment;

    private MeetupReviewStatus status;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
