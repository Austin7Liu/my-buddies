package com.austin.module.risk.domain;

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
@TableName("account_business_restriction_audit_log")
public class AccountBusinessRestrictionAuditLog {

    @TableId
    private Long id;

    private Long restrictionId;

    private Long operatorAccountId;

    private RestrictionAuditAction action;

    private String reason;

    private LocalDateTime occurredAt;
}
