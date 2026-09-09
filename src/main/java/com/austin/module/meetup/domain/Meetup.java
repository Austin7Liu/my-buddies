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
@TableName("meetup")
public class Meetup {

    @TableId
    private Long id;

    private Long creatorAccountId;

    private Long topicId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long circleId;

    private MeetupMode meetupMode;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime applicationDeadline;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String city;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String district;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String locationName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String address;

    private Integer capacity;

    private Integer minimumAge;

    private Integer maximumAge;

    private GenderRequirement genderRequirement;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String skillRequirement;

    private MeetupStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String closedReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long closedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime closedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
