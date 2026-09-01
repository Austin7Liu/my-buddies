package com.austin.module.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.mapper.UserAccountMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountServiceTests {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Test
    void createsActiveAccount() {
        UserAccount account = userAccountService.create("13800138000");

        assertThat(account.getId()).isPositive();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getVersion()).isZero();
        assertThat(account.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicatePhone() {
        userAccountService.create("13800138001");

        assertThatThrownBy(() -> userAccountService.create("13800138001"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该手机号已绑定账户");
    }

    @Test
    void requestsAndRevokesCancellation() {
        UserAccount account = userAccountService.create("13800138002");

        UserAccount pending = userAccountService.requestCancellation(account.getId());
        assertThat(pending.getAccountStatus()).isEqualTo(AccountStatus.CANCEL_PENDING);
        assertThat(pending.getCancelRequestedAt()).isNotNull();
        assertThat(pending.getVersion()).isEqualTo(1);

        UserAccount active = userAccountService.revokeCancellation(account.getId());
        assertThat(active.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(active.getCancelRequestedAt()).isNull();
        assertThat(active.getVersion()).isEqualTo(2);
    }

    @Test
    void completesCancellationAndReleasesPhoneAfterCoolingOffPeriod() {
        String phone = "13800138003";
        UserAccount account = userAccountService.create(phone);
        userAccountService.requestCancellation(account.getId());
        userAccountMapper.update(new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, account.getId())
                .set(UserAccount::getCancelRequestedAt, LocalDateTime.now().minusDays(8)));

        UserAccount cancelled = userAccountService.completeCancellation(account.getId());
        assertThat(cancelled.getAccountStatus()).isEqualTo(AccountStatus.CANCELLED);
        assertThat(cancelled.getPhone()).isNull();
        assertThat(cancelled.getCancelledAt()).isNotNull();

        assertThat(userAccountService.create(phone).getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
