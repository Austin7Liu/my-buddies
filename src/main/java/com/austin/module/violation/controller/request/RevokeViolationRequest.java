package com.austin.module.violation.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeViolationRequest(
        @NotBlank
        @Size(max = 500)
        String reason,

        boolean revokeLinkedRestrictions) {
}
