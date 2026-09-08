package com.austin.module.profile.controller.response;

import com.austin.module.account.domain.AccountStatus;
import com.austin.module.identity.domain.Gender;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.profile.domain.AvatarCode;
import com.austin.module.profile.domain.UserProfile;
import com.austin.module.profile.service.ProfileService.ProfileView;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record ProfileResponse(
        Long accountId,
        String nickname,
        AvatarCode avatarCode,
        String bio,
        String city,
        String district,
        boolean verified,
        Integer age,
        Gender gender,
        boolean accountRestricted,
        LocalDateTime joinedAt,
        LocalDateTime updatedAt) {

    public static ProfileResponse from(ProfileView view, Clock clock) {
        UserProfile profile = view.profile();
        boolean verified = view.identity() != null
                && view.identity().getStatus() == IdentityStatus.VERIFIED;
        LocalDate birthDate = verified ? view.identity().getBirthDate() : null;
        Integer age = birthDate == null ? null
                : Period.between(birthDate, LocalDate.now(clock)).getYears();
        return new ProfileResponse(
                view.account().getId(),
                profile == null ? defaultNickname(view.account().getId()) : profile.getNickname(),
                profile == null ? AvatarCode.PANDA : profile.getAvatarCode(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getCity(),
                profile == null ? null : profile.getDistrict(),
                verified,
                age,
                verified ? view.identity().getGender() : null,
                view.account().getAccountStatus() == AccountStatus.BANNED,
                view.account().getCreatedAt(),
                profile == null ? view.account().getUpdatedAt() : profile.getUpdatedAt());
    }

    private static String defaultNickname(long accountId) {
        return "伙伴_" + String.format("%06d", accountId % 1_000_000);
    }
}
