package com.austin.module.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.mapper.SearchOutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class SearchOutboxServiceTests {

    @Autowired
    private SearchOutboxService outboxService;

    @Autowired
    private SearchOutboxEventMapper mapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void outboxEventRollsBackWithOwningTransaction() {
        long before = mapper.selectCount(null);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            outboxService.recordRefresh(SearchDocumentType.POST, 999L);
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(mapper.selectCount(null)).isEqualTo(before);
    }
}
