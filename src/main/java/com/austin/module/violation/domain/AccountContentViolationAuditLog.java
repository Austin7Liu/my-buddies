package com.austin.module.violation.domain;

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
@TableName("account_content_violation_audit_log")
public class AccountContentViolationAuditLog {

    @TableId
    private Long id;

    private Long violationId;

    private Long operatorAccountId;

    private ViolationAuditAction action;

    private String reason;

    private LocalDateTime occurredAt;
}
