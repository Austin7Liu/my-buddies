package com.austin.module.post.mapper;

import com.austin.module.post.domain.PostLike;
import com.austin.module.post.mapper.model.PostLikeCountRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    List<PostLikeCountRow> selectCounts(@Param("postIds") Collection<Long> postIds);

    List<Long> selectLikedPostIds(
            @Param("accountId") long accountId,
            @Param("postIds") Collection<Long> postIds);
}
