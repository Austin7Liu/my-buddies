package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.ParticipantRole;
import com.austin.module.meetup.domain.ParticipantStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record MeetupParticipantResponse(
        Long accountId,
        ProfileSummaryResponse profile,
        ParticipantRole role,
        ParticipantStatus status,
        String applicationMessage,
        String decisionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MeetupParticipantResponse from(MeetupParticipant participant, ProfileSummaryResponse profile) {
        return new MeetupParticipantResponse(participant.getAccountId(), profile, participant.getRole(),
                participant.getStatus(), participant.getApplicationMessage(), participant.getDecisionReason(),
                participant.getCreatedAt(), participant.getUpdatedAt());
    }
}
