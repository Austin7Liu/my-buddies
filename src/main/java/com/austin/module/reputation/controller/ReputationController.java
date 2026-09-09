package com.austin.module.reputation.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.reputation.controller.response.ReputationResponse;
import com.austin.module.reputation.service.ReputationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profiles")
public class ReputationController {

    private final ReputationService reputationService;

    @GetMapping("/{accountId}/reputation")
    public ApiResponse<ReputationResponse> getPublic(@PathVariable @Positive long accountId) {
        return ApiResponse.success(ReputationResponse.from(reputationService.getPublic(accountId)));
    }
}
