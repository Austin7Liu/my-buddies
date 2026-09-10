package com.austin.module.catalog.service;

import com.austin.common.exception.ForbiddenException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.domain.TopicFollow;
import com.austin.module.catalog.mapper.TopicFollowMapper;
import com.austin.module.catalog.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TopicFollowServiceImpl implements TopicFollowService {

    private final TopicFollowMapper followMapper;
    private final TopicMapper topicMapper;
    private final CatalogService catalogService;
    private final UserAccountService accountService;
    private final Clock clock;

    @Override
    @Transactional
    public boolean follow(long accountId, long topicId) {
        ensureAccountActive(accountId);
        catalogService.getTopic(topicId, false);
        try {
            followMapper.insert(TopicFollow.builder()
                    .topicId(topicId)
                    .accountId(accountId)
                    .createdAt(LocalDateTime.now(clock))
                    .build());
        } catch (DuplicateKeyException ignored) {
            // PUT is idempotent; the existing relationship is already the requested state.
        }
        return true;
    }

    @Override
    @Transactional
    public boolean unfollow(long accountId, long topicId) {
        ensureAccountActive(accountId);
        followMapper.delete(new LambdaQueryWrapper<TopicFollow>()
                .eq(TopicFollow::getTopicId, topicId)
                .eq(TopicFollow::getAccountId, accountId));
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(long accountId, long topicId) {
        return followMapper.selectCount(new LambdaQueryWrapper<TopicFollow>()
                .eq(TopicFollow::getTopicId, topicId)
                .eq(TopicFollow::getAccountId, accountId)) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> followedTopicIds(long accountId, Collection<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return Set.of();
        }
        return followMapper.selectList(new LambdaQueryWrapper<TopicFollow>()
                        .eq(TopicFollow::getAccountId, accountId)
                        .in(TopicFollow::getTopicId, topicIds))
                .stream()
                .map(TopicFollow::getTopicId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Topic> listMine(long accountId, long page, long size) {
        return topicMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Topic>()
                .eq(Topic::getEnabled, true)
                .apply("EXISTS (SELECT 1 FROM category c WHERE c.id = topic.category_id AND c.enabled = TRUE)")
                .apply("EXISTS (SELECT 1 FROM topic_follow tf WHERE tf.topic_id = topic.id "
                        + "AND tf.account_id = {0})", accountId)
                .orderByAsc(Topic::getSortOrder)
                .orderByAsc(Topic::getId));
    }

    private void ensureAccountActive(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("账户状态不允许操作话题关注");
        }
    }
}
