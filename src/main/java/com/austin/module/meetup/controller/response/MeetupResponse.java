package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.GenderRequirement;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.meetup.domain.MeetupOnlineDetail;
import com.austin.module.meetup.domain.MeetupStatus;
import java.time.LocalDateTime;

public record MeetupResponse(
        Long id,
        Long creatorAccountId,
        Long topicId,
        Long circleId,
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
        String onlinePlatform,
        String serverRegion,
        String accessInstructions,
        Integer capacity,
        long acceptedCount,
        long remainingSlots,
        Integer minimumAge,
        Integer maximumAge,
        GenderRequirement genderRequirement,
        String skillRequirement,
        MeetupStatus status,
        String closedReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MeetupResponse from(
            Meetup meetup,
            MeetupOnlineDetail onlineDetail,
            long acceptedCount,
            boolean exposePrivateDetails) {
        return new MeetupResponse(meetup.getId(), meetup.getCreatorAccountId(), meetup.getTopicId(),
                meetup.getCircleId(), meetup.getMeetupMode(), meetup.getTitle(), meetup.getDescription(), meetup.getStartTime(),
                meetup.getEndTime(), meetup.getApplicationDeadline(), meetup.getCity(), meetup.getDistrict(),
                meetup.getLocationName(), exposePrivateDetails ? meetup.getAddress() : null,
                onlineDetail == null ? null : onlineDetail.getOnlinePlatform(),
                onlineDetail == null ? null : onlineDetail.getServerRegion(),
                onlineDetail == null || !exposePrivateDetails ? null : onlineDetail.getAccessInstructions(),
                meetup.getCapacity(),
                acceptedCount, Math.max(0, meetup.getCapacity() - acceptedCount), meetup.getMinimumAge(),
                meetup.getMaximumAge(), meetup.getGenderRequirement(), meetup.getSkillRequirement(),
                meetup.getStatus(), meetup.getClosedReason(), meetup.getCreatedAt(), meetup.getUpdatedAt());
    }
}
