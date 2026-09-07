package com.austin.module.meetup.controller.request;

import jakarta.validation.constraints.Size;

public record MeetupApplicationRequest(
        @Size(max = 500, message = "申请说明不能超过 500 个字符")
        String message) {
}
