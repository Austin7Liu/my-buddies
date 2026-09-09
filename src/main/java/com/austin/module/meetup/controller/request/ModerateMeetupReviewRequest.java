package com.austin.module.meetup.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerateMeetupReviewRequest(
        @NotBlank
        @Size(max = 255)
        String reason) {
}
