package com.austin.module.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.config.AccountProperties;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.mapper.UserAccountMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class AccountCancellationProcessorTests {

    @Test
    void failuresAndConcurrentRevocationDoNotBlockNextBatch() {
        UserAccountMapper mapper = mock(UserAccountMapper.class);
        UserAccountService service = mock(UserAccountService.class);
        LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 1, 0, 0);
        List<UserAccount> firstBatch = LongStream.rangeClosed(1, 100)
                .mapToObj(id -> UserAccount.builder().id(id).cancelRequestedAt(requestedAt).build())
                .toList();
        UserAccount afterFailure = UserAccount.builder().id(101L).cancelRequestedAt(requestedAt).build();
        when(mapper.selectCancellationDueAccounts(any(), isNull(), isNull(), anyInt()))
                .thenReturn(firstBatch);
        when(mapper.selectCancellationDueAccounts(any(), any(), any(), anyInt()))
                .thenReturn(List.of(afterFailure));
        doThrow(new IllegalStateException("test failure")).when(service).completeCancellation(1L);
        doThrow(new ConflictException("revoked concurrently")).when(service).completeCancellation(2L);

        AccountCancellationProcessor processor = new AccountCancellationProcessor(
                mapper, service, new AccountProperties(Duration.ofDays(7)),
                Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC));
        processor.completeDueAccounts();

        verify(mapper).selectCancellationDueAccounts(any(), any(), any(), anyInt());
        verify(service).completeCancellation(101L);
    }
}
