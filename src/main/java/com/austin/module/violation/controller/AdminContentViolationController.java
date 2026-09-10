package com.austin.module.violation.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.violation.controller.request.ConfirmViolationRequest;
import com.austin.module.violation.controller.request.RevokeViolationRequest;
import com.austin.module.violation.controller.response.ContentViolationResponse;
import com.austin.module.violation.service.ContentViolationService;
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
@RequestMapping("/api/v1/admin")
public class AdminContentViolationController {

    private final ContentViolationService service;

    @PostMapping("/content-reports/{reportId}/violations")
    public ApiResponse<ContentViolationResponse> confirm(
            Authentication authentication,
            @PathVariable @Positive long reportId,
            @Valid @RequestBody ConfirmViolationRequest request) {
        var view = service.confirm(accountId(authentication), reportId, request.severity(),
                request.penaltyType(), request.penaltyExpiresAt(), request.note());
        return ApiResponse.success(ContentViolationResponse.from(
                view.violation(), view.linkedRestrictionIds()));
    }

    @PostMapping("/content-violations/{violationId}/revoke")
    public ApiResponse<ContentViolationResponse> revoke(
            Authentication authentication,
            @PathVariable @Positive long violationId,
            @Valid @RequestBody RevokeViolationRequest request) {
        var view = service.revoke(accountId(authentication), violationId, request.reason(),
                request.revokeLinkedRestrictions());
        return ApiResponse.success(ContentViolationResponse.from(
                view.violation(), view.linkedRestrictionIds()));
    }

    @GetMapping("/content-violations")
    public ApiResponse<PageResponse<ContentViolationResponse>> list(
            @RequestParam @Positive long accountId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(service.listForAdmin(accountId, page, size),
                ContentViolationResponse::from));
    }

    @GetMapping("/content-violations/{violationId}")
    public ApiResponse<ContentViolationResponse> get(@PathVariable @Positive long violationId) {
        var view = service.getForAdmin(violationId);
        return ApiResponse.success(ContentViolationResponse.from(
                view.violation(), view.linkedRestrictionIds()));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
