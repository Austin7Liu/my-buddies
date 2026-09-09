package com.austin.module.post.mapper;

import com.austin.module.post.domain.PostComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
}
