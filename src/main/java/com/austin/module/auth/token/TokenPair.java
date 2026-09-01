package com.austin.module.auth.token;

public record TokenPair(String accessToken, String refreshToken, long accessExpiresInSeconds) {
}
