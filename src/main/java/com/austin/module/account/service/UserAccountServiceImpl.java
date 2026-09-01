package com.austin.module.account.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.config.AccountProperties;
import com.austin.module.account.mapper.UserAccountMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountMapper userAccountMapper;
    private final Clock clock;
    private final AccountProperties accountProperties;

    @Override
    @Transactional
    public UserAccount create(String phone) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserAccount account = UserAccount.builder()
                .phone(phone.trim())
                .accountStatus(AccountStatus.ACTIVE)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            userAccountMapper.insert(account);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("该手机号已绑定账户", exception);
        }
        return account;
    }

    @Override
    @Transactional
    public UserAccount findOrCreateByPhone(String phone) {
        String normalizedPhone = phone.trim();
        UserAccount existing = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getPhone, normalizedPhone));
        if (existing != null) {
            return existing;
        }
        try {
            return create(normalizedPhone);
        } catch (ConflictException exception) {
            UserAccount concurrentlyCreated = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                    .eq(UserAccount::getPhone, normalizedPhone));
            if (concurrentlyCreated != null) {
                return concurrentlyCreated;
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount getById(long accountId) {
        UserAccount account = userAccountMapper.selectById(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("用户账户不存在");
        }
        return account;
    }

    @Override
    @Transactional
    public UserAccount requestCancellation(long accountId) {
        UserAccount account = getById(accountId);
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("只有正常账户可以申请注销");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int affected = userAccountMapper.update(new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, accountId)
                .eq(UserAccount::getAccountStatus, AccountStatus.ACTIVE)
                .set(UserAccount::getAccountStatus, AccountStatus.CANCEL_PENDING)
                .set(UserAccount::getCancelRequestedAt, now)
                .set(UserAccount::getUpdatedAt, now)
                .setSql("version = version + 1"));
        if (affected != 1) {
            throw new ConflictException("账户状态已发生变化，请刷新后重试");
        }
        return getById(accountId);
    }

    /**
     * 用户在 7 天注销冷静期内撤销注销申请
     */
    @Override
    @Transactional
    public UserAccount revokeCancellation(long accountId) {
        UserAccount account = getById(accountId);
        if (account.getAccountStatus() != AccountStatus.CANCEL_PENDING) {
            throw new ConflictException("账户不处于注销冷静期");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int affected = userAccountMapper.update(new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, accountId)
                .eq(UserAccount::getAccountStatus, AccountStatus.CANCEL_PENDING)
                .set(UserAccount::getAccountStatus, AccountStatus.ACTIVE)
                .set(UserAccount::getCancelRequestedAt, null)
                .set(UserAccount::getUpdatedAt, now)
                .setSql("version = version + 1"));
        if (affected != 1) {
            throw new ConflictException("账户状态已发生变化，请刷新后重试");
        }
        return getById(accountId);
    }

    @Override
    @Transactional
    public UserAccount completeCancellation(long accountId) {
        UserAccount account = getById(accountId);
        if (account.getAccountStatus() != AccountStatus.CANCEL_PENDING) {
            throw new ConflictException("账户不处于注销冷静期");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cancellableAt = account.getCancelRequestedAt()
                .plus(accountProperties.cancellationCoolingOffPeriod());
        if (now.isBefore(cancellableAt)) {
            throw new ConflictException("账户仍处于注销冷静期");
        }
        int affected = userAccountMapper.update(new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, accountId)
                .eq(UserAccount::getAccountStatus, AccountStatus.CANCEL_PENDING)
                .eq(UserAccount::getCancelRequestedAt, account.getCancelRequestedAt())
                .set(UserAccount::getAccountStatus, AccountStatus.CANCELLED)
                .set(UserAccount::getPhone, null)
                .set(UserAccount::getCancelledAt, now)
                .set(UserAccount::getUpdatedAt, now)
                .setSql("version = version + 1"));
        if (affected != 1) {
            throw new ConflictException("账户状态已发生变化，请刷新后重试");
        }
        return getById(accountId);
    }
}
