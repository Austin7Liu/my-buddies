package com.austin.module.search.mapper;

import com.austin.module.search.domain.SearchOutboxEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SearchOutboxEventMapper extends BaseMapper<SearchOutboxEvent> {
}
