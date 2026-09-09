package com.austin.module.meetup.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("meetup_review_audit_log")
public class MeetupReviewAuditLog {

    @TableId
    private Long id;

    private Long reviewId;

    private Long operatorAccountId;

    private MeetupReviewAuditAction action;

    private String reason;

    private LocalDateTime occurredAt;
}
