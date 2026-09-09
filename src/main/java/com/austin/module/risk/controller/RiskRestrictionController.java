package com.austin.module.risk.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.risk.controller.response.RestrictionResponse;
import com.austin.module.risk.service.RiskRestrictionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/risk/restrictions")
public class RiskRestrictionController {

    private final RiskRestrictionService restrictionService;
    private final Clock clock;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<RestrictionResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        LocalDateTime now = LocalDateTime.now(clock);
        return ApiResponse.success(PageResponse.from(
                restrictionService.listMine(accountId(authentication), page, size),
                restriction -> RestrictionResponse.from(restriction, now)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
