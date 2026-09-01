package com.austin.module.identity.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.identity.config.IdentityProperties;
import com.austin.module.identity.controller.request.SubmitIdentityVerificationRequest;
import com.austin.module.identity.controller.response.IdentityVerificationResponse;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.identity.service.IdentityVerificationService;
import jakarta.validation.Valid;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity-verification")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;
    private final IdentityProperties identityProperties;
    private final Clock clock;

    @GetMapping("/me")
    public ApiResponse<IdentityVerificationResponse> getMine(Authentication authentication) {
        IdentityVerification verification = identityVerificationService.findByAccountId(accountId(authentication));
        return ApiResponse.success(verification == null
                ? IdentityVerificationResponse.unverified()
                : response(verification));
    }

    @PostMapping
    public ApiResponse<IdentityVerificationResponse> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitIdentityVerificationRequest request) {
        IdentityVerification verification = identityVerificationService.submit(
                accountId(authentication), request.realName(), request.identityNumber());
        return ApiResponse.success(response(verification));
    }

    private IdentityVerificationResponse response(IdentityVerification verification) {
        return IdentityVerificationResponse.from(verification, clock, identityProperties.minimumAge());
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}

