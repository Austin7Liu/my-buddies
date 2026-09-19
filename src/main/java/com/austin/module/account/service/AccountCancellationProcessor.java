package com.austin.module.account.service;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.config.AccountProperties;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.mapper.UserAccountMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCancellationProcessor {

    private static final int BATCH_SIZE = 100;

    private final UserAccountMapper accountMapper;
    private final UserAccountService accountService;
    private final AccountProperties accountProperties;
    private final Clock clock;

    @Scheduled(initialDelayString = "${app.account.cancellation-poll-delay:1h}",
            fixedDelayString = "${app.account.cancellation-poll-delay:1h}")
    public void completeDueAccounts() {
        LocalDateTime cutoff = LocalDateTime.now(clock)
                .minus(accountProperties.cancellationCoolingOffPeriod());
        LocalDateTime cursorAt = null;
        Long cursorId = null;
        while (true) {
            List<UserAccount> accounts = accountMapper.selectCancellationDueAccounts(
                    cutoff, cursorAt, cursorId, BATCH_SIZE);
            if (accounts.isEmpty()) {
                return;
            }
            for (UserAccount account : accounts) {
                try {
                    accountService.completeCancellation(account.getId());
                } catch (ConflictException exception) {
                    log.debug("Cancellation state changed concurrently. accountId={}", account.getId());
                } catch (RuntimeException exception) {
                    log.error("Failed to complete account cancellation. accountId={}", account.getId(), exception);
                }
            }
            UserAccount last = accounts.getLast();
            cursorAt = last.getCancelRequestedAt();
            cursorId = last.getId();
            if (accounts.size() < BATCH_SIZE) {
                return;
            }
        }
    }
}
