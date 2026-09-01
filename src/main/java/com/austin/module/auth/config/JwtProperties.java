package com.austin.module.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenExpiration,
                            Duration refreshTokenExpiration) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer 不能为空");
        }
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret 至少需要 32 个字符");
        }
        if (accessTokenExpiration == null || accessTokenExpiration.isNegative()
                || accessTokenExpiration.isZero()) {
            throw new IllegalArgumentException("访问令牌有效期必须大于 0");
        }
        if (refreshTokenExpiration == null || refreshTokenExpiration.isNegative()
                || refreshTokenExpiration.isZero()) {
            throw new IllegalArgumentException("刷新令牌有效期必须大于 0");
        }
    }
}
