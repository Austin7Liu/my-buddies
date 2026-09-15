package com.austin.module.search.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.austin.common.exception.InvalidRequestException;
import com.austin.module.search.provider.SearchProvider;
import org.junit.jupiter.api.Test;

class SearchServiceTests {

    private final SearchProvider searchProvider = mock(SearchProvider.class);
    private final SearchService searchService = new SearchService(searchProvider);

    @Test
    void delegatesOrdinarySearchToProvider() {
        SearchCriteria criteria = new SearchCriteria("网球", null, null, null, 1, 20);

        searchService.search(criteria);

        verify(searchProvider).search(criteria);
    }

    @Test
    void rejectsDeepPagination() {
        SearchCriteria criteria = new SearchCriteria("网球", null, null, null, 51, 20);

        assertThatThrownBy(() -> searchService.search(criteria))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("1000");
    }
}
