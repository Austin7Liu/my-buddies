package com.austin.module.admin.domain;

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
@TableName("admin_role_audit_log")
public class AdminRoleAuditLog {

    @TableId
    private Long id;
    private Long operatorAccountId;
    private Long targetAccountId;
    private AdminRoleCode roleCode;
    private AdminRoleAuditAction action;
    private LocalDateTime occurredAt;
}
