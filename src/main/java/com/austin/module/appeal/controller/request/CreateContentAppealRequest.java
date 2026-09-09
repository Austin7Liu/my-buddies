package com.austin.module.appeal.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateContentAppealRequest(
        @Positive
        long reportId,

        @NotBlank
        @Size(max = 1000)
        String reason) {
}
