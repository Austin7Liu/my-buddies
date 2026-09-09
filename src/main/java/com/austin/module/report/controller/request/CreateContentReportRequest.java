package com.austin.module.report.controller.request;

import com.austin.module.report.domain.ReportReasonType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContentReportRequest(
        @NotNull
        ReportReasonType reasonType,

        @Size(max = 500)
        String description) {
}
