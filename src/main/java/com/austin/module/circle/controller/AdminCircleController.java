package com.austin.module.circle.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.circle.controller.request.RejectCircleRequest;
import com.austin.module.circle.controller.response.CircleResponse;
import com.austin.module.circle.domain.CircleStatus;
import com.austin.module.circle.service.CircleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/admin/circles")
public class AdminCircleController {
    private final CircleService circleService;

    @GetMapping
    public ApiResponse<PageResponse<CircleResponse>> list(
            @RequestParam(required = false) CircleStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(circleService.listForReview(status, page, size), CircleResponse::from));
    }

    @PostMapping("/{circleId}/approve")
    public ApiResponse<CircleResponse> approve(Authentication auth, @PathVariable @Positive long circleId) {
        return ApiResponse.success(CircleResponse.from(circleService.approve(accountId(auth), circleId)));
    }

    @PostMapping("/{circleId}/reject")
    public ApiResponse<CircleResponse> reject(Authentication auth, @PathVariable @Positive long circleId,
            @Valid @RequestBody RejectCircleRequest request) {
        return ApiResponse.success(CircleResponse.from(circleService.reject(accountId(auth), circleId, request.reason())));
    }

    @PatchMapping("/{circleId}/enabled")
    public ApiResponse<CircleResponse> setEnabled(Authentication auth, @PathVariable @Positive long circleId,
            @RequestParam boolean enabled) {
        return ApiResponse.success(CircleResponse.from(circleService.setEnabled(accountId(auth), circleId, enabled)));
    }

    private long accountId(Authentication auth) { return Long.parseLong(auth.getName()); }
}

