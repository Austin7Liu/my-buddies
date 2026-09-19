package com.austin.module.auth.controller.response;

import com.austin.module.account.controller.response.UserAccountResponse;
import com.austin.module.auth.service.AuthService.AuthResult;
import java.time.Duration;

public record LoginResponse(
        UserAccountResponse account,
        TokenResponse tokens) {

    public static LoginResponse from(AuthResult result, Duration coolingOffPeriod) {
        return new LoginResponse(UserAccountResponse.from(result.account(), coolingOffPeriod),
                TokenResponse.from(result.tokens()));
    }
}
