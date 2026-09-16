package com.austin.module.meetup.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.meetup.check-in")
public record MeetupCheckInProperties(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal maxAccuracyMeters) {
}
