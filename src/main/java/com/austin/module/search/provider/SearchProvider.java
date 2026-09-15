package com.austin.module.search.provider;

import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.service.ReindexResult;
import com.austin.module.search.service.SearchCriteria;
import com.austin.module.search.service.SearchPage;
import java.util.List;

public interface SearchProvider {

    SearchPage search(SearchCriteria criteria);

    ReindexResult rebuild(List<SearchDocument> documents);

    void upsert(SearchDocument document);

    void delete(SearchDocumentType type, long businessId);
}
