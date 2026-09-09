package com.austin.module.meetup.controller.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MeetupCheckInRequest(
        @NotNull(message = "纬度不能为空")
        @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
        @DecimalMax(value = "90.0", message = "纬度不能大于 90")
        BigDecimal latitude,
        @NotNull(message = "经度不能为空")
        @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
        @DecimalMax(value = "180.0", message = "经度不能大于 180")
        BigDecimal longitude) {
}
