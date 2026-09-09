package com.austin.module.meetup.service;

import com.austin.module.meetup.domain.GenderRequirement;
import com.austin.module.meetup.domain.MeetupMode;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record MeetupCommand(
        MeetupMode meetupMode,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime applicationDeadline,
        String city,
        String district,
        String locationName,
        String address,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        Integer checkInRadiusMeters,
        String onlinePlatform,
        String serverRegion,
        String accessInstructions,
        int capacity,
        int minimumAge,
        int maximumAge,
        GenderRequirement genderRequirement,
        String skillRequirement) {
}
