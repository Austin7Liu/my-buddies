package com.austin.module.report.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ReportStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ContentReportResponseTests {

    @Test
    void includesAppealDeadlineInAdminAndUserViews() {
        LocalDateTime deadline = LocalDateTime.of(2026, 9, 20, 12, 0);
        ContentReport report = ContentReport.builder()
                .id(1L)
                .status(ReportStatus.RESOLVED)
                .appealDeadlineAt(deadline)
                .build();

        assertThat(ContentReportResponse.from(report).appealDeadlineAt()).isEqualTo(deadline);
        assertThat(ContentReportResponse.fromAdmin(report).appealDeadlineAt()).isEqualTo(deadline);
    }
}
