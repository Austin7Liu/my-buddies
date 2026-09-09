package com.austin.module.appeal.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.appeal.controller.request.ReviewContentAppealRequest;
import com.austin.module.appeal.controller.response.ContentAppealResponse;
import com.austin.module.appeal.domain.ContentAppealStatus;
import com.austin.module.appeal.service.ContentAppealService;
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
@RequestMapping("/api/v1/admin/content-appeals")
public class AdminContentAppealController {

    private final ContentAppealService appealService;

    @GetMapping
    public ApiResponse<PageResponse<ContentAppealResponse>> list(
            @RequestParam(required = false) ContentAppealStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(appealService.listForAdmin(status, page, size),
                ContentAppealResponse::from));
    }

    @GetMapping("/{appealId}")
    public ApiResponse<ContentAppealResponse> get(@PathVariable @Positive long appealId) {
        return ApiResponse.success(ContentAppealResponse.from(appealService.getForAdmin(appealId)));
    }

    @PostMapping("/{appealId}/approve")
    public ApiResponse<ContentAppealResponse> approve(
            Authentication authentication,
            @PathVariable @Positive long appealId,
            @Valid @RequestBody ReviewContentAppealRequest request) {
        return ApiResponse.success(ContentAppealResponse.from(appealService.approve(
                accountId(authentication), appealId, request.reviewNote())));
    }

    @PostMapping("/{appealId}/reject")
    public ApiResponse<ContentAppealResponse> reject(
            Authentication authentication,
            @PathVariable @Positive long appealId,
            @Valid @RequestBody ReviewContentAppealRequest request) {
        return ApiResponse.success(ContentAppealResponse.from(appealService.reject(
                accountId(authentication), appealId, request.reviewNote())));
    }

    @PostMapping("/{appealId}/close")
    public ApiResponse<ContentAppealResponse> close(
            Authentication authentication,
            @PathVariable @Positive long appealId,
            @Valid @RequestBody ReviewContentAppealRequest request) {
        return ApiResponse.success(ContentAppealResponse.from(appealService.close(
                accountId(authentication), appealId, request.reviewNote())));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
