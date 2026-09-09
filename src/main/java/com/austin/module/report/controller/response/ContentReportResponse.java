package com.austin.module.report.controller.response;

import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ReportReasonType;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
import java.time.LocalDateTime;

public record ContentReportResponse(
        Long id,
        Long reporterAccountId,
        ReportTargetType targetType,
        Long reportedPostId,
        Long reportedCommentId,
        ReportReasonType reasonType,
        String description,
        String contentSnapshot,
        ReportStatus status,
        Long handledBy,
        LocalDateTime handledAt,
        String resolutionNote,
        LocalDateTime createdAt) {

    public static ContentReportResponse from(ContentReport report) {
        return from(report, false);
    }

    public static ContentReportResponse fromAdmin(ContentReport report) {
        return from(report, true);
    }

    private static ContentReportResponse from(ContentReport report, boolean includeSnapshot) {
        return new ContentReportResponse(report.getId(), report.getReporterAccountId(),
                report.getTargetType(), report.getReportedPostId(), report.getReportedCommentId(),
                report.getReasonType(), report.getDescription(),
                includeSnapshot ? report.getContentSnapshot() : null,
                report.getStatus(), report.getHandledBy(), report.getHandledAt(),
                report.getResolutionNote(), report.getCreatedAt());
    }
}
