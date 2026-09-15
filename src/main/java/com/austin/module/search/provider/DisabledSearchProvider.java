package com.austin.module.search.provider;

import com.austin.common.exception.SearchTemporarilyUnavailableException;
import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.service.ReindexResult;
import com.austin.module.search.service.SearchCriteria;
import com.austin.module.search.service.SearchPage;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "false")
public class DisabledSearchProvider implements SearchProvider {

    @Override
    public SearchPage search(SearchCriteria criteria) {
        throw unavailable();
    }

    @Override
    public ReindexResult rebuild(List<SearchDocument> documents) {
        throw unavailable();
    }

    @Override
    public void upsert(SearchDocument document) {
        throw unavailable();
    }

    @Override
    public void delete(SearchDocumentType type, long businessId) {
        throw unavailable();
    }

    private SearchTemporarilyUnavailableException unavailable() {
        return new SearchTemporarilyUnavailableException("搜索服务当前未启用");
    }
}
