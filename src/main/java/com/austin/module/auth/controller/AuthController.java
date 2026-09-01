package com.austin.module.auth.controller;

import com.austin.common.exception.UnauthorizedException;
import com.austin.common.model.ApiResponse;
import com.austin.module.auth.controller.request.LogoutRequest;
import com.austin.module.auth.controller.request.RefreshTokenRequest;
import com.austin.module.auth.controller.request.SendSmsCodeRequest;
import com.austin.module.auth.controller.request.SmsLoginRequest;
import com.austin.module.auth.controller.response.LoginResponse;
import com.austin.module.auth.controller.response.SmsCodeResponse;
import com.austin.module.auth.controller.response.TokenResponse;
import com.austin.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sms-codes")
    public ResponseEntity<ApiResponse<SmsCodeResponse>> sendSmsCode(
            @Valid @RequestBody SendSmsCodeRequest request) {
        long expiresInSeconds = authService.sendVerificationCode(request.phone());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new SmsCodeResponse(expiresInSeconds)));
    }

    @PostMapping("/token")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody SmsLoginRequest request) {
        return ApiResponse.success(LoginResponse.from(
                authService.loginOrRegister(request.phone(), request.code())));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(TokenResponse.from(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(Long.parseLong(authentication.getName()), bearerToken(authorization),
                request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少访问令牌");
        }
        return authorization.substring(7);
    }
}
