package com.austin.module.meetup.controller.response;

import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.ParticipantRole;
import com.austin.module.meetup.domain.ParticipantStatus;
import java.time.LocalDateTime;

public record MeetupParticipantResponse(
        Long accountId,
        ParticipantRole role,
        ParticipantStatus status,
        String applicationMessage,
        String decisionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MeetupParticipantResponse from(MeetupParticipant participant) {
        return new MeetupParticipantResponse(participant.getAccountId(), participant.getRole(),
                participant.getStatus(), participant.getApplicationMessage(), participant.getDecisionReason(),
                participant.getCreatedAt(), participant.getUpdatedAt());
    }
}
