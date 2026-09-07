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
@TableName("meetup_participant")
public class MeetupParticipant {

    @TableId
    private Long id;

    private Long meetupId;

    private Long accountId;

    private ParticipantRole role;

    private ParticipantStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String applicationMessage;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String decisionReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime decidedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime cancelledAt;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
