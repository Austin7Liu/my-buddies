package com.austin.module.circle.domain;

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
@TableName("circle_audit_log")
public class CircleAuditLog {

    @TableId
    private Long id;

    private Long circleId;

    private Long operatorAccountId;

    private CircleAuditAction action;

    private CircleStatus fromStatus;

    private CircleStatus toStatus;

    private String reason;

    private LocalDateTime occurredAt;
}
