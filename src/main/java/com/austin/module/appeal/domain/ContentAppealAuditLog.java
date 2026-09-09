package com.austin.module.appeal.domain;

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
@TableName("content_appeal_audit_log")
public class ContentAppealAuditLog {

    @TableId
    private Long id;

    private Long appealId;

    private Long operatorAccountId;

    private ContentAppealAuditAction action;

    private ContentAppealStatus fromStatus;

    private ContentAppealStatus toStatus;

    private String note;

    private LocalDateTime occurredAt;
}
