package com.austin.module.violation.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.violation.controller.response.ContentViolationResponse;
import com.austin.module.violation.service.ContentViolationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/violations")
public class ContentViolationController {

    private final ContentViolationService service;

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ContentViolationResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(service.listMine(
                accountId(authentication), page, size), ContentViolationResponse::from));
    }

    @GetMapping("/{violationId}")
    public ApiResponse<ContentViolationResponse> getMine(
            Authentication authentication,
            @PathVariable @Positive long violationId) {
        var view = service.getMine(accountId(authentication), violationId);
        return ApiResponse.success(ContentViolationResponse.from(
                view.violation(), view.linkedRestrictionIds()));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
