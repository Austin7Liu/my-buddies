package com.austin.module.post.service;

import com.austin.module.post.domain.Post;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Collection;
import java.util.Map;

public interface PostInteractionService {

    InteractionSummary like(long accountId, long postId);

    InteractionSummary unlike(long accountId, long postId);

    InteractionSummary bookmark(long accountId, long postId);

    InteractionSummary removeBookmark(long accountId, long postId);

    IPage<Post> listBookmarkedPosts(long accountId, long page, long size);

    Map<Long, InteractionSummary> summarize(Collection<Long> postIds, Long accountId);

    record InteractionSummary(
            long likeCount,
            boolean likedByMe,
            boolean bookmarkedByMe) {
    }
}
