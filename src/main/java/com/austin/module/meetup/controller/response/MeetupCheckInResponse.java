package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.MeetupCheckIn;
import java.time.LocalDateTime;

public record MeetupCheckInResponse(
        Long id,
        Long meetupId,
        Long accountId,
        Integer distanceMeters,
        LocalDateTime checkedInAt) {

    public static MeetupCheckInResponse from(MeetupCheckIn value) {
        return new MeetupCheckInResponse(value.getId(), value.getMeetupId(), value.getAccountId(),
                value.getDistanceMeters(), value.getCheckedInAt());
    }
}
