package com.austin.module.profile.service;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.profile.domain.AvatarCode;
import com.austin.module.profile.domain.UserProfile;

public interface ProfileService {

    ProfileView getPublic(long accountId);

    ProfileView getMine(long accountId);

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
}
