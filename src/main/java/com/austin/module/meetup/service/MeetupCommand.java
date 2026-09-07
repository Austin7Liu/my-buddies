package com.austin.module.meetup.service;

import com.austin.module.meetup.domain.GenderRequirement;
import java.time.LocalDateTime;

public record MeetupCommand(
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime applicationDeadline,
        String city,
        String district,
        String locationName,
        String address,
        int capacity,
        int minimumAge,
        int maximumAge,
        GenderRequirement genderRequirement,
        String skillRequirement) {
}
