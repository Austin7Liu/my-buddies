package com.austin.module.search.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.austin.common.exception.SearchTemporarilyUnavailableException;
import com.austin.module.search.service.SearchCriteria;
import org.junit.jupiter.api.Test;

class DisabledSearchProviderTests {

    private final DisabledSearchProvider provider = new DisabledSearchProvider();

    @Test
    void reportsStableUnavailableErrorWhenSearchIsDisabled() {
        SearchCriteria criteria = new SearchCriteria("网球", null, null, null, 1, 20);

        assertThatThrownBy(() -> provider.search(criteria))
                .isInstanceOf(SearchTemporarilyUnavailableException.class)
                .hasMessage("搜索服务当前未启用");
    }
}
