package com.austin.module.catalog.service;

import com.austin.module.catalog.domain.Topic;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Collection;
import java.util.Set;

public interface TopicFollowService {

    boolean follow(long accountId, long topicId);

    boolean unfollow(long accountId, long topicId);

    boolean isFollowing(long accountId, long topicId);

    Set<Long> followedTopicIds(long accountId, Collection<Long> topicIds);

    IPage<Topic> listMine(long accountId, long page, long size);
}
