package com.austin.module.profile.mapper.model;

import com.austin.module.account.domain.AccountStatus;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.profile.domain.AvatarCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileSummaryRow {

    private Long accountId;

    private String nickname;

    private AvatarCode avatarCode;

    private IdentityStatus identityStatus;

    private AccountStatus accountStatus;
}
