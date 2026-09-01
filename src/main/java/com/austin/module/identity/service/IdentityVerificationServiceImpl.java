package com.austin.module.identity.service;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.identity.mapper.IdentityVerificationMapper;
import com.austin.module.identity.provider.IdentityProviderResult;
import com.austin.module.identity.provider.IdentityVerificationCommand;
import com.austin.module.identity.provider.IdentityVerificationProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityVerificationServiceImpl implements IdentityVerificationService {

    private final IdentityVerificationMapper identityVerificationMapper;
    private final IdentityVerificationProvider identityVerificationProvider;
    private final UserAccountService userAccountService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public IdentityVerification findByAccountId(long accountId) {
        return identityVerificationMapper.selectOne(new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getAccountId, accountId));
    }

    @Override
    @Transactional
    public IdentityVerification submit(long accountId, String realName, String identityNumber) {
        UserAccount account = userAccountService.getById(accountId);
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("只有正常账户可以提交实名认证");
        }

        IdentityVerification verification = findByAccountId(accountId);
        if (verification != null && verification.getStatus() == IdentityStatus.VERIFIED) {
            throw new ConflictException("账户已完成实名认证");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (verification == null) {
            verification = IdentityVerification.builder()
                    .accountId(accountId)
                    .status(IdentityStatus.VERIFYING)
                    .provider("PENDING")
                    .submittedAt(now)
                    .version(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            identityVerificationMapper.insert(verification);
        } else {
            verification.setStatus(IdentityStatus.VERIFYING);
            clearResult(verification);
            verification.setProvider("PENDING");
            verification.setSubmittedAt(now);
            verification.setUpdatedAt(now);
            if (identityVerificationMapper.updateById(verification) != 1) {
                throw new ConflictException("实名认证状态已发生变化，请刷新后重试");
            }
        }

        IdentityProviderResult result = identityVerificationProvider.verify(
                new IdentityVerificationCommand(realName, identityNumber));
        applyResult(verification, result, now);
        try {
            if (identityVerificationMapper.updateById(verification) != 1) {
                throw new ConflictException("实名认证状态已发生变化，请刷新后重试");
            }
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("该实名主体已绑定其他账户", exception);
        }
        return verification;
    }

    private void applyResult(IdentityVerification verification, IdentityProviderResult result,
            LocalDateTime now) {
        verification.setProvider(result.provider());
        verification.setProviderReference(result.providerReference());
        verification.setUpdatedAt(now);
        if (result.verified()) {
            verification.setStatus(IdentityStatus.VERIFIED);
            verification.setSubjectFingerprint(result.subjectFingerprint());
            verification.setBirthDate(result.birthDate());
            verification.setGender(result.gender());
            verification.setFailureCode(null);
            verification.setVerifiedAt(now);
        } else {
            verification.setStatus(IdentityStatus.FAILED);
            verification.setFailureCode(result.failureCode());
        }
    }

    private void clearResult(IdentityVerification verification) {
        verification.setSubjectFingerprint(null);
        verification.setBirthDate(null);
        verification.setGender(null);
        verification.setProviderReference(null);
        verification.setFailureCode(null);
        verification.setVerifiedAt(null);
    }
}

