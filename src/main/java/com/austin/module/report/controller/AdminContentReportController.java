package com.austin.module.report.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.report.controller.request.HandleContentReportRequest;
import com.austin.module.report.controller.response.ContentReportResponse;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.service.ContentReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequestMapping("/api/v1/admin/content-reports")
public class AdminContentReportController {

    private final ContentReportService reportService;

    @GetMapping
    public ApiResponse<PageResponse<ContentReportResponse>> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(reportService.listForAdmin(status, page, size),
                ContentReportResponse::fromAdmin));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ContentReportResponse> get(@PathVariable @Positive long reportId) {
        return ApiResponse.success(ContentReportResponse.fromAdmin(reportService.getForAdmin(reportId)));
    }

    @PostMapping("/{reportId}/resolve")
    public ApiResponse<ContentReportResponse> resolve(
            Authentication authentication,
            @PathVariable @Positive long reportId,
            @Valid @RequestBody HandleContentReportRequest request) {
        return ApiResponse.success(ContentReportResponse.fromAdmin(reportService.resolve(
                accountId(authentication), reportId, request.resolutionNote())));
    }

    @PostMapping("/{reportId}/reject")
    public ApiResponse<ContentReportResponse> reject(
            Authentication authentication,
            @PathVariable @Positive long reportId,
            @Valid @RequestBody HandleContentReportRequest request) {
        return ApiResponse.success(ContentReportResponse.fromAdmin(reportService.reject(
                accountId(authentication), reportId, request.resolutionNote())));
    }

    @PostMapping("/{reportId}/duplicate")
    public ApiResponse<ContentReportResponse> duplicate(
            Authentication authentication,
            @PathVariable @Positive long reportId,
            @Valid @RequestBody HandleContentReportRequest request) {
        return ApiResponse.success(ContentReportResponse.fromAdmin(reportService.markDuplicate(
                accountId(authentication), reportId, request.resolutionNote())));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
