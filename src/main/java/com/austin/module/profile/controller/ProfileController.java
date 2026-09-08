package com.austin.module.profile.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.profile.controller.request.UpdateProfileRequest;
import com.austin.module.profile.controller.response.ProfileResponse;
import com.austin.module.profile.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final Clock clock;

    @GetMapping("/{accountId}")
    public ApiResponse<ProfileResponse> getPublic(
            @PathVariable
            @Positive
            long accountId) {
        return ApiResponse.success(ProfileResponse.from(profileService.getPublic(accountId), clock));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileResponse> getMine(Authentication authentication) {
        return ApiResponse.success(ProfileResponse.from(profileService.getMine(accountId(authentication)), clock));
    }

    @PutMapping("/me")
    public ApiResponse<ProfileResponse> update(
            Authentication authentication,
            @Valid
            @RequestBody
            UpdateProfileRequest request) {
        return ApiResponse.success(ProfileResponse.from(profileService.update(
                accountId(authentication), request.nickname(), request.avatarCode(), request.bio(),
                request.city(), request.district()), clock));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
