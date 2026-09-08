package com.austin.module.profile.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.profile.domain.AvatarCode;
import com.austin.module.profile.domain.UserProfile;
import com.austin.module.profile.mapper.UserProfileMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileMapper profileMapper;
    private final UserAccountService accountService;
    private final IdentityVerificationService identityService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ProfileView getPublic(long accountId) {
        UserAccount account = visibleAccount(accountId);
        return view(account, profileMapper.selectById(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileView getMine(long accountId) {
        UserAccount account = visibleAccount(accountId);
        return view(account, profileMapper.selectById(accountId));
    }

    @Override
    @Transactional
    public ProfileView update(
            long accountId,
            String nickname,
            AvatarCode avatarCode,
            String bio,
            String city,
            String district) {
        UserAccount account = accountService.getById(accountId);
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("当前账户状态不允许修改资料");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        UserProfile profile = profileMapper.selectById(accountId);
        if (profile == null) {
            profile = UserProfile.builder()
                    .accountId(accountId)
                    .nickname(nickname.trim())
                    .avatarCode(avatarCode)
                    .bio(trim(bio))
                    .city(trim(city))
                    .district(trim(district))
                    .version(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            try {
                profileMapper.insert(profile);
            } catch (DuplicateKeyException exception) {
                throw new ConflictException("用户资料已发生变化，请刷新后重试", exception);
            }
        } else {
            profile.setNickname(nickname.trim());
            profile.setAvatarCode(avatarCode);
            profile.setBio(trim(bio));
            profile.setCity(trim(city));
            profile.setDistrict(trim(district));
            profile.setUpdatedAt(now);
            if (profileMapper.updateById(profile) != 1) {
                throw new ConflictException("用户资料已发生变化，请刷新后重试");
            }
        }
        return view(account, profile);
    }

    private UserAccount visibleAccount(long accountId) {
        UserAccount account = accountService.getById(accountId);
        if (account.getAccountStatus() == AccountStatus.CANCELLED) {
            throw new ResourceNotFoundException("用户资料不存在");
        }
        return account;
    }

    private ProfileView view(UserAccount account, UserProfile profile) {
        return new ProfileView(account, profile, identityService.findByAccountId(account.getId()));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
