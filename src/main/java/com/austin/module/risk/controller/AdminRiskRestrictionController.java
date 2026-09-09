package com.austin.module.risk.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.risk.controller.request.CreateRestrictionRequest;
import com.austin.module.risk.controller.request.RevokeRestrictionRequest;
import com.austin.module.risk.controller.response.RestrictionResponse;
import com.austin.module.risk.service.RiskRestrictionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.LocalDateTime;
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
@PreAuthorize("hasAnyRole('RISK_REVIEWER', 'SUPER_ADMIN')")
@RequestMapping("/api/v1/admin/risk")
public class AdminRiskRestrictionController {

    private final RiskRestrictionService restrictionService;
    private final Clock clock;

    @PostMapping("/restrictions")
    public ApiResponse<RestrictionResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateRestrictionRequest request) {
        return ApiResponse.success(RestrictionResponse.from(restrictionService.create(
                accountId(authentication), request.accountId(), request.restrictionType(),
                request.reason(), request.expiresAt()), LocalDateTime.now(clock)));
    }

    @GetMapping("/accounts/{accountId}/restrictions")
    public ApiResponse<PageResponse<RestrictionResponse>> list(
            @PathVariable @Positive long accountId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        LocalDateTime now = LocalDateTime.now(clock);
        return ApiResponse.success(PageResponse.from(
                restrictionService.listForAdmin(accountId, page, size),
                restriction -> RestrictionResponse.from(restriction, now)));
    }

    @PostMapping("/restrictions/{restrictionId}/revoke")
    public ApiResponse<RestrictionResponse> revoke(
            Authentication authentication,
            @PathVariable @Positive long restrictionId,
            @Valid @RequestBody RevokeRestrictionRequest request) {
        return ApiResponse.success(RestrictionResponse.from(restrictionService.revoke(
                accountId(authentication), restrictionId, request.reason()), LocalDateTime.now(clock)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
