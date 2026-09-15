package com.austin.module.search.service;

import com.austin.module.search.domain.SearchDocument;

public record SearchHitResult(
        SearchDocument document,
        Double score,
        String titleHighlight,
        String contentHighlight) {
}
