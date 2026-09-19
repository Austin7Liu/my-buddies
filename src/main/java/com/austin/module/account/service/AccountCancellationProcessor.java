package com.austin.module.account.service;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.config.AccountProperties;
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
        List<Long> accountIds = accountMapper.selectCancellationDueIds(cutoff, BATCH_SIZE);
        for (long accountId : accountIds) {
            try {
                accountService.completeCancellation(accountId);
            } catch (ConflictException exception) {
                log.debug("Cancellation state changed concurrently. accountId={}", accountId);
            } catch (RuntimeException exception) {
                log.error("Failed to complete account cancellation. accountId={}", accountId, exception);
            }
        }
    }
}
