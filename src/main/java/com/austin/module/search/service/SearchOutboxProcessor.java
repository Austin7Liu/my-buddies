package com.austin.module.search.service;

import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.domain.SearchOutboxEvent;
import com.austin.module.search.domain.SearchOutboxEventType;
import com.austin.module.search.domain.SearchOutboxStatus;
import com.austin.module.search.mapper.SearchOutboxEventMapper;
import com.austin.module.search.provider.SearchProvider;
import com.austin.module.search.config.SearchProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchOutboxProcessor {

    private final SearchOutboxEventMapper mapper;
    private final SearchDocumentFactory documentFactory;
    private final SearchProvider searchProvider;
    private final SearchCascadeService cascadeService;
    private final SearchProperties properties;
    private final Clock clock;
    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${app.search.outbox-poll-delay:2s}")
    public void processPending() {
        LocalDateTime now = LocalDateTime.now(clock);
        recoverTimedOut(now);
        List<SearchOutboxEvent> events = mapper.selectList(new LambdaQueryWrapper<SearchOutboxEvent>()
                .eq(SearchOutboxEvent::getStatus, SearchOutboxStatus.PENDING)
                .le(SearchOutboxEvent::getNextRetryAt, now)
                .orderByAsc(SearchOutboxEvent::getCreatedAt)
                .last("LIMIT " + properties.outboxBatchSize()));
        events.forEach(event -> processIfClaimed(event, now));
    }

    private void processIfClaimed(SearchOutboxEvent event, LocalDateTime now) {
        int claimed = mapper.update(null, new LambdaUpdateWrapper<SearchOutboxEvent>()
                .eq(SearchOutboxEvent::getId, event.getId())
                .eq(SearchOutboxEvent::getStatus, SearchOutboxStatus.PENDING)
                .set(SearchOutboxEvent::getStatus, SearchOutboxStatus.PROCESSING)
                .set(SearchOutboxEvent::getLockedBy, workerId)
                .set(SearchOutboxEvent::getLockedAt, now));
        if (claimed != 1) {
            return;
        }
        try {
            if (event.getEventType() == SearchOutboxEventType.CASCADE_REFRESH) {
                cascadeService.expand(event.getAggregateType(), event.getAggregateId());
                markSucceeded(event.getId());
                return;
            }
            Optional<SearchDocument> document = documentFactory.build(event.getAggregateType(), event.getAggregateId());
            if (document.isPresent()) {
                searchProvider.upsert(document.get());
            } else {
                searchProvider.delete(event.getAggregateType(), event.getAggregateId());
            }
            markSucceeded(event.getId());
        } catch (Exception exception) {
            markFailed(event, exception);
        }
    }

    private void markSucceeded(long eventId) {
        mapper.update(null, new LambdaUpdateWrapper<SearchOutboxEvent>()
                .eq(SearchOutboxEvent::getId, eventId)
                .eq(SearchOutboxEvent::getStatus, SearchOutboxStatus.PROCESSING)
                .eq(SearchOutboxEvent::getLockedBy, workerId)
                .set(SearchOutboxEvent::getStatus, SearchOutboxStatus.SUCCEEDED)
                .set(SearchOutboxEvent::getProcessedAt, LocalDateTime.now(clock))
                .set(SearchOutboxEvent::getLockedBy, null)
                .set(SearchOutboxEvent::getLockedAt, null));
    }

    private void markFailed(SearchOutboxEvent event, Exception exception) {
        int retryCount = event.getRetryCount() + 1;
        boolean dead = retryCount >= properties.outboxMaxRetries();
        LocalDateTime now = LocalDateTime.now(clock);
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        mapper.update(null, new LambdaUpdateWrapper<SearchOutboxEvent>()
                .eq(SearchOutboxEvent::getId, event.getId())
                .eq(SearchOutboxEvent::getStatus, SearchOutboxStatus.PROCESSING)
                .eq(SearchOutboxEvent::getLockedBy, workerId)
                .set(SearchOutboxEvent::getStatus, dead ? SearchOutboxStatus.DEAD : SearchOutboxStatus.PENDING)
                .set(SearchOutboxEvent::getRetryCount, retryCount)
                .set(SearchOutboxEvent::getNextRetryAt, now.plusSeconds(retryDelaySeconds(retryCount)))
                .set(SearchOutboxEvent::getLastError, message.substring(0, Math.min(message.length(), 500)))
                .set(SearchOutboxEvent::getLockedBy, null)
                .set(SearchOutboxEvent::getLockedAt, null));
    }

    private void recoverTimedOut(LocalDateTime now) {
        mapper.update(null, new LambdaUpdateWrapper<SearchOutboxEvent>()
                .eq(SearchOutboxEvent::getStatus, SearchOutboxStatus.PROCESSING)
                .le(SearchOutboxEvent::getLockedAt, now.minus(properties.outboxLockTimeout()))
                .set(SearchOutboxEvent::getStatus, SearchOutboxStatus.PENDING)
                .set(SearchOutboxEvent::getNextRetryAt, now)
                .set(SearchOutboxEvent::getLockedBy, null)
                .set(SearchOutboxEvent::getLockedAt, null));
    }

    private long retryDelaySeconds(int retryCount) {
        return switch (retryCount) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 120;
            default -> 600;
        };
    }
}
