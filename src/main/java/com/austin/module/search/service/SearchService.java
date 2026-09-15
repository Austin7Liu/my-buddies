package com.austin.module.search.service;

import com.austin.common.exception.InvalidRequestException;
import com.austin.module.search.provider.SearchProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchProvider searchProvider;

    public SearchPage search(SearchCriteria criteria) {
        if ((long) (criteria.page() - 1) * criteria.size() >= 1000) {
            throw new InvalidRequestException("搜索暂不支持超过 1000 条的深分页");
        }
        return searchProvider.search(criteria);
    }
}
