package com.austin.module.report.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.report.controller.request.CreateContentReportRequest;
import com.austin.module.report.controller.response.ContentReportResponse;
import com.austin.module.report.service.ContentReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/reports")
public class ContentReportController {

    private final ContentReportService reportService;

    @PostMapping("/posts/{postId}")
    public ApiResponse<ContentReportResponse> reportPost(
            Authentication authentication,
            @PathVariable @Positive long postId,
            @Valid @RequestBody CreateContentReportRequest request) {
        return ApiResponse.success(ContentReportResponse.from(reportService.reportPost(
                accountId(authentication), postId, request.reasonType(), request.description())));
    }

    @PostMapping("/post-comments/{commentId}")
    public ApiResponse<ContentReportResponse> reportComment(
            Authentication authentication,
            @PathVariable @Positive long commentId,
            @Valid @RequestBody CreateContentReportRequest request) {
        return ApiResponse.success(ContentReportResponse.from(reportService.reportComment(
                accountId(authentication), commentId, request.reasonType(), request.description())));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ContentReportResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                reportService.listMine(accountId(authentication), page, size),
                ContentReportResponse::from));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ContentReportResponse> getMine(
            Authentication authentication,
            @PathVariable @Positive long reportId) {
        return ApiResponse.success(ContentReportResponse.from(
                reportService.getMine(accountId(authentication), reportId)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
