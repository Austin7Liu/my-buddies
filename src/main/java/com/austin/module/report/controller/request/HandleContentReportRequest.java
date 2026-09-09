package com.austin.module.report.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HandleContentReportRequest(
        @NotBlank
        @Size(max = 500)
        String resolutionNote) {
}
