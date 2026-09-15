package com.austin.module.search.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.config.SearchProperties;
import com.austin.module.search.domain.SearchOutboxEventType;
import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.domain.SearchOutboxEvent;
import com.austin.module.search.domain.SearchOutboxStatus;
import com.austin.module.search.mapper.SearchOutboxEventMapper;
import com.austin.module.search.provider.SearchProvider;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

class SearchOutboxProcessorTests {

    @Test
    void claimedVisiblePostIsUpserted() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SearchOutboxEvent.class);
        SearchOutboxEventMapper mapper = mock(SearchOutboxEventMapper.class);
        SearchDocumentFactory factory = mock(SearchDocumentFactory.class);
        SearchProvider provider = mock(SearchProvider.class);
        SearchCascadeService cascadeService = mock(SearchCascadeService.class);
        SearchProperties properties = new SearchProperties(true, "search", 200, Duration.ofSeconds(2), 100, 5,
                Duration.ofMinutes(5));
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T06:00:00Z"), ZoneOffset.UTC);
        SearchOutboxEvent event = SearchOutboxEvent.builder()
                .id(1L)
                .aggregateType(SearchDocumentType.POST)
                .aggregateId(10L)
                .eventType(SearchOutboxEventType.REFRESH)
                .status(SearchOutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now(clock))
                .createdAt(LocalDateTime.now(clock))
                .build();
        SearchDocument document = new SearchDocument("POST:10", SearchDocumentType.POST, 10L, null, "网球",
                null, null, null, null, 1L, null, null, null, null,
                LocalDateTime.now(clock), LocalDateTime.now(clock), true);
        when(mapper.selectList(any())).thenReturn(List.of(event));
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(factory.build(SearchDocumentType.POST, 10L)).thenReturn(Optional.of(document));

        new SearchOutboxProcessor(mapper, factory, provider, cascadeService, properties, clock).processPending();

        verify(provider).upsert(document);
    }
}
