package com.austin.module.post.mapper;

import com.austin.module.post.domain.PostBookmark;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostBookmarkMapper extends BaseMapper<PostBookmark> {

    List<Long> selectBookmarkedPostIds(
            @Param("accountId") long accountId,
            @Param("postIds") Collection<Long> postIds);
}
