package com.austin.module.search.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(
        boolean enabled,

        @NotBlank
        String indexAlias,

        @Min(1)
        int batchSize,

        Duration outboxPollDelay,

        @Min(1)
        int outboxBatchSize,

        @Min(1)
        int outboxMaxRetries,

        Duration outboxLockTimeout) {
}
