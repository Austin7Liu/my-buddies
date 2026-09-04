package com.austin.common.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> records, long page, long size, long total, long pages) {
    public static <S, T> PageResponse<T> from(IPage<S> source, Function<S, T> mapper) {
        return new PageResponse<>(source.getRecords().stream().map(mapper).toList(),
                source.getCurrent(), source.getSize(), source.getTotal(), source.getPages());
    }
}

