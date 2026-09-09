package com.austin.module.appeal.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.appeal.controller.request.CreateContentAppealRequest;
import com.austin.module.appeal.controller.response.ContentAppealResponse;
import com.austin.module.appeal.service.ContentAppealService;
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
@RequestMapping("/api/v1/content-appeals")
public class ContentAppealController {

    private final ContentAppealService appealService;

    @PostMapping
    public ApiResponse<ContentAppealResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateContentAppealRequest request) {
        return ApiResponse.success(ContentAppealResponse.from(appealService.create(
                accountId(authentication), request.reportId(), request.reason())));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ContentAppealResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                appealService.listMine(accountId(authentication), page, size),
                ContentAppealResponse::from));
    }

    @GetMapping("/{appealId}")
    public ApiResponse<ContentAppealResponse> getMine(
            Authentication authentication,
            @PathVariable @Positive long appealId) {
        return ApiResponse.success(ContentAppealResponse.from(
                appealService.getMine(accountId(authentication), appealId)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
