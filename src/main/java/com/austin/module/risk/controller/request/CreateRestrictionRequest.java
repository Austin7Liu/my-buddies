package com.austin.module.risk.controller.request;

import com.austin.module.risk.domain.RestrictionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateRestrictionRequest(
        @NotNull
        @Positive
        Long accountId,

        @NotNull
        RestrictionType restrictionType,

        @NotBlank
        @Size(max = 255)
        String reason,

        @NotNull
        LocalDateTime expiresAt) {
}
