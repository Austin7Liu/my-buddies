package com.austin.module.post.mapper;

import com.austin.module.post.domain.PostCommentAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostCommentAuditLogMapper extends BaseMapper<PostCommentAuditLog> {
}
