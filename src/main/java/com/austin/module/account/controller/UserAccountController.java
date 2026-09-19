package com.austin.module.account.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.account.config.AccountProperties;
import com.austin.module.account.controller.response.UserAccountResponse;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final AccountProperties accountProperties;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserAccountResponse> getMine(Authentication authentication) {
        return response(userAccountService.getById(accountId(authentication)));
    }

    @PostMapping("/me/cancellation")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserAccountResponse> requestMyCancellation(Authentication authentication) {
        return response(userAccountService.requestCancellation(accountId(authentication)));
    }

    @PostMapping("/me/cancellation/revoke")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserAccountResponse> revokeMyCancellation(Authentication authentication) {
        return response(userAccountService.revokeCancellation(accountId(authentication)));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("#accountId.toString() == authentication.name")
    public ApiResponse<UserAccountResponse> getById(@PathVariable @Positive long accountId) {
        return response(userAccountService.getById(accountId));
    }

    @PostMapping("/{accountId}/cancellation")
    @PreAuthorize("#accountId.toString() == authentication.name")
    public ApiResponse<UserAccountResponse> requestCancellation(@PathVariable @Positive long accountId) {
        return response(userAccountService.requestCancellation(accountId));
    }

    @PostMapping("/{accountId}/cancellation/revoke")
    @PreAuthorize("#accountId.toString() == authentication.name")
    public ApiResponse<UserAccountResponse> revokeCancellation(@PathVariable @Positive long accountId) {
        return response(userAccountService.revokeCancellation(accountId));
    }

    private ApiResponse<UserAccountResponse> response(UserAccount account) {
        return ApiResponse.success(UserAccountResponse.from(account,
                accountProperties.cancellationCoolingOffPeriod()));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
