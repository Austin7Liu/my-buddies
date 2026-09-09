package com.austin.module.meetup.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMeetupReviewRequest(
        @NotNull
        @Positive
        Long revieweeAccountId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Size(max = 500)
        String comment) {
}
