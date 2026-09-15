package com.austin.module.search.service;

import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.domain.SearchOutboxEvent;
import com.austin.module.search.domain.SearchOutboxEventType;
import com.austin.module.search.domain.SearchOutboxStatus;
import com.austin.module.search.mapper.SearchOutboxEventMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchOutboxService {

    private final SearchOutboxEventMapper mapper;
    private final Clock clock;

    public void recordRefresh(SearchDocumentType type, long aggregateId) {
        record(type, aggregateId, SearchOutboxEventType.REFRESH);
    }

    public void recordCascade(SearchDocumentType type, long aggregateId) {
        record(type, aggregateId, SearchOutboxEventType.CASCADE_REFRESH);
    }

    private void record(SearchDocumentType type, long aggregateId, SearchOutboxEventType eventType) {
        LocalDateTime now = LocalDateTime.now(clock);
        mapper.insert(SearchOutboxEvent.builder()
                .aggregateType(type)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .status(SearchOutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(now)
                .createdAt(now)
                .build());
    }
}
