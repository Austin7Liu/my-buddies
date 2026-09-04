package com.austin.module.post.mapper;

import com.austin.module.post.domain.PostAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostAuditLogMapper extends BaseMapper<PostAuditLog> {
}
