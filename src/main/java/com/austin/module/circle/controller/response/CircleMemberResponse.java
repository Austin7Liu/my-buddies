package com.austin.module.circle.controller.response;

import com.austin.module.circle.domain.CircleMember;
import com.austin.module.circle.domain.CircleMemberRole;
import com.austin.module.circle.domain.CircleMemberStatus;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import java.time.LocalDateTime;

public record CircleMemberResponse(
        Long circleId,
        ProfileSummaryResponse account,
        CircleMemberRole role,
        CircleMemberStatus status,
        LocalDateTime joinedAt,
        LocalDateTime leftAt) {

    public static CircleMemberResponse from(CircleMember member, ProfileSummaryResponse account) {
        return new CircleMemberResponse(
                member.getCircleId(),
                account,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getLeftAt());
    }
}
