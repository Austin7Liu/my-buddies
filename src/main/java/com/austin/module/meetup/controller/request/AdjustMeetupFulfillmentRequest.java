package com.austin.module.meetup.controller.request;

import com.austin.module.meetup.domain.FulfillmentResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustMeetupFulfillmentRequest(
        @NotNull
        FulfillmentResult result,

        @NotBlank
        @Size(max = 255)
        String reason) {
}
