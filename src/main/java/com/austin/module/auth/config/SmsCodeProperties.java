package com.austin.module.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.sms-code")
public record SmsCodeProperties(Duration expiration, Duration resendCooldown,
                                int maxFailedAttempts, Duration lockDuration) {

    public SmsCodeProperties {
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("验证码有效期必须大于 0");
        }
        if (resendCooldown == null || resendCooldown.isNegative() || resendCooldown.isZero()) {
            throw new IllegalArgumentException("验证码重发间隔必须大于 0");
        }
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("验证码最大错误次数必须大于 0");
        }
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("验证码锁定时间必须大于 0");
        }
    }
}
