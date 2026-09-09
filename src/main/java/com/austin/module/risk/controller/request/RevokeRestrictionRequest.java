package com.austin.module.risk.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeRestrictionRequest(
        @NotBlank
        @Size(max = 255)
        String reason) {
}
