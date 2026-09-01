package com.austin.module.account.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.account.service.UserAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserAccountResponse>> create(
            @Valid @RequestBody CreateUserAccountRequest request) {
        UserAccountResponse response = UserAccountResponse.from(userAccountService.create(request.phone()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{accountId}")
    public ApiResponse<UserAccountResponse> getById(@PathVariable @Positive long accountId) {
        return ApiResponse.success(UserAccountResponse.from(userAccountService.getById(accountId)));
    }

    @PostMapping("/{accountId}/cancellation")
    public ApiResponse<UserAccountResponse> requestCancellation(@PathVariable @Positive long accountId) {
        return ApiResponse.success(UserAccountResponse.from(userAccountService.requestCancellation(accountId)));
    }

    @PostMapping("/{accountId}/cancellation/revoke")
    public ApiResponse<UserAccountResponse> revokeCancellation(@PathVariable @Positive long accountId) {
        return ApiResponse.success(UserAccountResponse.from(userAccountService.revokeCancellation(accountId)));
    }
}
