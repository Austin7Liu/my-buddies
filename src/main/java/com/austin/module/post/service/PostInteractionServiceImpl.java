package com.austin.module.post.service;

import com.austin.common.exception.ForbiddenException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostBookmark;
import com.austin.module.post.domain.PostLike;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.mapper.PostBookmarkMapper;
import com.austin.module.post.mapper.PostLikeMapper;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.post.mapper.model.PostLikeCountRow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final PostLikeMapper likeMapper;
    private final PostBookmarkMapper bookmarkMapper;
    private final PostMapper postMapper;
    private final PostService postService;
    private final UserAccountService accountService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional
    public InteractionSummary like(long accountId, long postId) {
        ensureAccountActive(accountId);
        Post post = postService.getPublic(postId);
        boolean created = insertLike(accountId, postId);
        if (created && !post.getAuthorAccountId().equals(accountId)) {
            notificationService.notify(
                    post.getAuthorAccountId(),
                    NotificationType.POST_LIKED,
                    "帖子收到点赞",
                    "有人点赞了你的帖子",
                    NotificationReferenceType.POST_LIKE,
                    postId,
                    "post-like:" + postId + ":" + accountId);
        }
        return summary(postId, accountId);
    }

    @Override
    @Transactional
    public InteractionSummary unlike(long accountId, long postId) {
        ensureAccountActive(accountId);
        likeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getAccountId, accountId));
        return summary(postId, accountId);
    }

    @Override
    @Transactional
    public InteractionSummary bookmark(long accountId, long postId) {
        ensureAccountActive(accountId);
        postService.getPublic(postId);
        try {
            bookmarkMapper.insert(PostBookmark.builder()
                    .postId(postId)
                    .accountId(accountId)
                    .createdAt(LocalDateTime.now(clock))
                    .build());
        } catch (DuplicateKeyException ignored) {
            // PUT is idempotent; an existing row already represents the requested state.
        }
        return summary(postId, accountId);
    }

    @Override
    @Transactional
    public InteractionSummary removeBookmark(long accountId, long postId) {
        ensureAccountActive(accountId);
        bookmarkMapper.delete(new LambdaQueryWrapper<PostBookmark>()
                .eq(PostBookmark::getPostId, postId)
                .eq(PostBookmark::getAccountId, accountId));
        return summary(postId, accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Post> listBookmarkedPosts(long accountId, long page, long size) {
        return postMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, PostStatus.PUBLISHED)
                .apply("EXISTS (SELECT 1 FROM post_bookmark pb WHERE pb.post_id = post.id "
                        + "AND pb.account_id = {0})", accountId)
                .and(value -> value.isNull(Post::getTopicId)
                        .or().apply("EXISTS (SELECT 1 FROM topic t JOIN category c ON c.id = t.category_id "
                                + "WHERE t.id = post.topic_id AND t.enabled = TRUE AND c.enabled = TRUE)"))
                .and(value -> value.isNull(Post::getCircleId)
                        .or().apply("EXISTS (SELECT 1 FROM circle ci WHERE ci.id = post.circle_id "
                                + "AND ci.status = 'APPROVED')"))
                .orderByDesc(Post::getCreatedAt)
                .orderByDesc(Post::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, InteractionSummary> summarize(Collection<Long> postIds, Long accountId) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = postIds.stream().distinct().toList();
        Map<Long, Long> counts = new HashMap<>();
        for (PostLikeCountRow row : likeMapper.selectCounts(ids)) {
            counts.put(row.getPostId(), row.getLikeCount());
        }
        Set<Long> liked = accountId == null
                ? Set.of()
                : new HashSet<>(likeMapper.selectLikedPostIds(accountId, ids));
        Set<Long> bookmarked = accountId == null
                ? Set.of()
                : new HashSet<>(bookmarkMapper.selectBookmarkedPostIds(accountId, ids));
        Map<Long, InteractionSummary> result = new HashMap<>();
        for (Long postId : ids) {
            result.put(postId, new InteractionSummary(
                    counts.getOrDefault(postId, 0L),
                    liked.contains(postId),
                    bookmarked.contains(postId)));
        }
        return Map.copyOf(result);
    }

    private boolean insertLike(long accountId, long postId) {
        try {
            likeMapper.insert(PostLike.builder()
                    .postId(postId)
                    .accountId(accountId)
                    .createdAt(LocalDateTime.now(clock))
                    .build());
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private InteractionSummary summary(long postId, long accountId) {
        return summarize(List.of(postId), accountId).get(postId);
    }

    private void ensureAccountActive(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("账户状态不允许操作帖子互动");
        }
    }
}
