package com.austin.module.auth.controller;

import com.austin.module.account.controller.UserAccountResponse;
import com.austin.module.auth.service.AuthService.AuthResult;

public record LoginResponse(UserAccountResponse account, TokenResponse tokens) {

    public static LoginResponse from(AuthResult result) {
        return new LoginResponse(UserAccountResponse.from(result.account()),
                TokenResponse.from(result.tokens()));
    }
}
