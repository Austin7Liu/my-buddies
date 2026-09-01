package com.austin.module.account.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.account")
public record AccountProperties(Duration cancellationCoolingOffPeriod) {

    public AccountProperties {
        if (cancellationCoolingOffPeriod == null || cancellationCoolingOffPeriod.isNegative()
                || cancellationCoolingOffPeriod.isZero()) {
            throw new IllegalArgumentException("账户注销冷静期必须大于 0");
        }
    }
}
