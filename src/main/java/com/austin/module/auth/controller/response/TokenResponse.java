package com.austin.module.auth.controller.response;

import com.austin.module.auth.token.TokenPair;

public record TokenResponse(String tokenType, String accessToken, String refreshToken,
                            long expiresInSeconds) {

    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse("Bearer", pair.accessToken(), pair.refreshToken(),
                pair.accessExpiresInSeconds());
    }
}
