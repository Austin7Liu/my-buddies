package com.austin.module.appeal.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewContentAppealRequest(
        @NotBlank
        @Size(max = 500)
        String reviewNote) {
}
