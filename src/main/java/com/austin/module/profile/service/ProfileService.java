package com.austin.module.profile.service;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.profile.domain.AvatarCode;
import com.austin.module.profile.domain.UserProfile;
import java.util.Collection;
import java.util.Map;

public interface ProfileService {

    ProfileView getPublic(long accountId);

    ProfileView getMine(long accountId);

    Map<Long, ProfileSummary> getSummaries(Collection<Long> accountIds);

    ProfileView update(
            long accountId,
            String nickname,
            AvatarCode avatarCode,
            String bio,
            String city,
            String district);

    record ProfileView(
            UserAccount account,
            UserProfile profile,
            IdentityVerification identity) {
    }

    record ProfileSummary(
            Long accountId,
            String nickname,
            AvatarCode avatarCode,
            boolean verified,
            boolean accountRestricted) {
    }
}
