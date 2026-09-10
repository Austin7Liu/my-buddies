package com.austin.module.violation.controller.request;

import com.austin.module.violation.domain.ViolationPenaltyType;
import com.austin.module.violation.domain.ViolationSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ConfirmViolationRequest(
        @NotNull
        ViolationSeverity severity,

        @NotNull
        ViolationPenaltyType penaltyType,

        LocalDateTime penaltyExpiresAt,

        @NotBlank
        @Size(max = 500)
        String note) {
}
