package com.austin.module.search.service;

import java.util.List;

public record SearchPage(
        List<SearchHitResult> records,
        long total,
        int page,
        int size,
        long pages) {
}
