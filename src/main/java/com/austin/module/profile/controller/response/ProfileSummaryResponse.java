package com.austin.module.profile.controller.response;

import com.austin.module.profile.domain.AvatarCode;
import com.austin.module.profile.service.ProfileService.ProfileSummary;

public record ProfileSummaryResponse(
        Long accountId,
        String nickname,
        AvatarCode avatarCode,
        boolean verified,
        boolean accountRestricted) {

    public static ProfileSummaryResponse from(ProfileSummary summary) {
        return summary == null ? null : new ProfileSummaryResponse(
                summary.accountId(), summary.nickname(), summary.avatarCode(),
                summary.verified(), summary.accountRestricted());
    }
}
