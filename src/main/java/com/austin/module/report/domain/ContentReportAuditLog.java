package com.austin.module.report.domain;

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
@TableName("content_report_audit_log")
public class ContentReportAuditLog {

    @TableId
    private Long id;

    private Long reportId;

    private Long operatorAccountId;

    private ReportAuditAction action;

    private ReportStatus fromStatus;

    private ReportStatus toStatus;

    private String note;

    private LocalDateTime occurredAt;
}
