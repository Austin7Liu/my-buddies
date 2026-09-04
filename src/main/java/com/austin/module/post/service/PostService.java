package com.austin.module.post.service;

import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface PostService {

    IPage<Post> listPublic(long page, long size);

    IPage<Post> listByTopic(long topicId, long page, long size);

    IPage<Post> listByCircle(long circleId, long page, long size);

    Post getPublic(long postId);

    IPage<Post> listMine(long authorId, PostStatus status, long page, long size);

    IPage<Post> listForReview(PostStatus status, long page, long size);

    Post create(long authorId, String content, Long topicId, Long circleId);

    Post update(long authorId, long postId, String content);

    Post delete(long authorId, long postId);

    Post approve(long moderatorId, long postId);

    Post reject(long moderatorId, long postId, String reason);

    Post offline(long moderatorId, long postId, String reason);

    Post restore(long moderatorId, long postId);
}
