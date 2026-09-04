package com.austin.module.post.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.service.CircleService;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.identity.policy.AgeEligibilityPolicy;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostAuditAction;
import com.austin.module.post.domain.PostAuditLog;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.mapper.PostAuditLogMapper;
import com.austin.module.post.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostAuditLogMapper auditMapper;
    private final UserAccountService accountService;
    private final IdentityVerificationService identityService;
    private final AgeEligibilityPolicy agePolicy;
    private final CatalogService catalogService;
    private final CircleService circleService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public IPage<Post> listPublic(long page, long size) {
        return selectPublic(new LambdaQueryWrapper<>(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Post> listByTopic(long topicId, long page, long size) {
        catalogService.getTopic(topicId, false);
        return selectPublic(new LambdaQueryWrapper<Post>().eq(Post::getTopicId, topicId), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Post> listByCircle(long circleId, long page, long size) {
        circleService.getPublic(circleId);
        return selectPublic(new LambdaQueryWrapper<Post>().eq(Post::getCircleId, circleId), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Post getPublic(long postId) {
        Post post = requirePost(postId);
        if (post.getStatus() != PostStatus.PUBLISHED || !associationsArePublic(post)) {
            throw new ResourceNotFoundException("帖子不存在");
        }
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Post> listMine(long authorId, PostStatus status, long page, long size) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorAccountId, authorId)
                .orderByDesc(Post::getCreatedAt)
                .orderByDesc(Post::getId);
        if (status != null) {
            query.eq(Post::getStatus, status);
        }
        return postMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Post> listForReview(PostStatus status, long page, long size) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<Post>()
                .orderByAsc(Post::getCreatedAt)
                .orderByAsc(Post::getId);
        if (status != null) {
            query.eq(Post::getStatus, status);
        }
        return postMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    @Transactional
    public Post create(long authorId, String content, Long topicId, Long circleId) {
        ensureAuthorEligible(authorId);
        Association association = resolveAssociation(topicId, circleId);
        LocalDateTime now = LocalDateTime.now(clock);
        Post post = Post.builder()
                .authorAccountId(authorId)
                .topicId(association.topicId())
                .circleId(association.circleId())
                .content(content.trim())
                .status(PostStatus.PENDING_REVIEW)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        postMapper.insert(post);
        audit(post.getId(), authorId, PostAuditAction.SUBMIT, null, PostStatus.PENDING_REVIEW, null, now);
        return post;
    }

    @Override
    @Transactional
    public Post update(long authorId, long postId, String content) {
        Post post = requireOwnedPost(authorId, postId);
        if (!EnumSet.of(PostStatus.PENDING_REVIEW, PostStatus.PUBLISHED, PostStatus.REJECTED)
                .contains(post.getStatus())) {
            throw new ConflictException("当前帖子状态不允许编辑");
        }
        ensureAuthorEligible(authorId);
        PostStatus from = post.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        post.setContent(content.trim());
        post.setStatus(PostStatus.PENDING_REVIEW);
        post.setModerationReason(null);
        post.setModeratedBy(null);
        post.setModeratedAt(null);
        post.setUpdatedAt(now);
        persist(post);
        audit(postId, authorId, from == PostStatus.REJECTED ? PostAuditAction.RESUBMIT : PostAuditAction.UPDATE,
                from, PostStatus.PENDING_REVIEW, null, now);
        return post;
    }

    @Override
    @Transactional
    public Post delete(long authorId, long postId) {
        Post post = requireOwnedPost(authorId, postId);
        if (post.getStatus() == PostStatus.DELETED) {
            throw new ConflictException("帖子已经删除");
        }
        PostStatus from = post.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        post.setStatus(PostStatus.DELETED);
        post.setDeletedAt(now);
        post.setUpdatedAt(now);
        persist(post);
        audit(postId, authorId, PostAuditAction.DELETE, from, PostStatus.DELETED, null, now);
        return post;
    }

    @Override
    @Transactional
    public Post approve(long moderatorId, long postId) {
        return review(moderatorId, postId, true, null);
    }

    @Override
    @Transactional
    public Post reject(long moderatorId, long postId, String reason) {
        return review(moderatorId, postId, false, reason.trim());
    }

    @Override
    @Transactional
    public Post offline(long moderatorId, long postId, String reason) {
        Post post = requirePost(postId);
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ConflictException("只有已发布帖子可以下架");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        post.setStatus(PostStatus.OFFLINE);
        post.setModerationReason(reason.trim());
        post.setModeratedBy(moderatorId);
        post.setModeratedAt(now);
        post.setUpdatedAt(now);
        persist(post);
        audit(postId, moderatorId, PostAuditAction.OFFLINE, PostStatus.PUBLISHED, PostStatus.OFFLINE,
                reason.trim(), now);
        return post;
    }

    @Override
    @Transactional
    public Post restore(long moderatorId, long postId) {
        Post post = requirePost(postId);
        if (post.getStatus() != PostStatus.OFFLINE) {
            throw new ConflictException("只有已下架帖子可以恢复");
        }
        validateAssociation(post.getTopicId(), post.getCircleId());
        LocalDateTime now = LocalDateTime.now(clock);
        post.setStatus(PostStatus.PUBLISHED);
        post.setModerationReason(null);
        post.setModeratedBy(moderatorId);
        post.setModeratedAt(now);
        post.setUpdatedAt(now);
        persist(post);
        audit(postId, moderatorId, PostAuditAction.RESTORE, PostStatus.OFFLINE, PostStatus.PUBLISHED, null, now);
        return post;
    }

    private Post review(long moderatorId, long postId, boolean approved, String reason) {
        Post post = requirePost(postId);
        if (post.getStatus() != PostStatus.PENDING_REVIEW) {
            throw new ConflictException("帖子不处于待审核状态");
        }
        if (approved) {
            validateAssociation(post.getTopicId(), post.getCircleId());
        }
        PostStatus target = approved ? PostStatus.PUBLISHED : PostStatus.REJECTED;
        LocalDateTime now = LocalDateTime.now(clock);
        post.setStatus(target);
        post.setModerationReason(reason);
        post.setModeratedBy(moderatorId);
        post.setModeratedAt(now);
        post.setUpdatedAt(now);
        persist(post);
        audit(postId, moderatorId, approved ? PostAuditAction.APPROVE : PostAuditAction.REJECT,
                PostStatus.PENDING_REVIEW, target, reason, now);
        return post;
    }

    private IPage<Post> selectPublic(LambdaQueryWrapper<Post> query, long page, long size) {
        query.eq(Post::getStatus, PostStatus.PUBLISHED)
                .and(value -> value.isNull(Post::getTopicId)
                        .or().apply("EXISTS (SELECT 1 FROM topic t JOIN category c ON c.id = t.category_id "
                                + "WHERE t.id = post.topic_id AND t.enabled = TRUE AND c.enabled = TRUE)"))
                .and(value -> value.isNull(Post::getCircleId)
                        .or().apply("EXISTS (SELECT 1 FROM circle ci WHERE ci.id = post.circle_id "
                                + "AND ci.status = 'APPROVED')"))
                .orderByDesc(Post::getCreatedAt)
                .orderByDesc(Post::getId);
        return postMapper.selectPage(new Page<>(page, size), query);
    }

    private Association resolveAssociation(Long topicId, Long circleId) {
        if (circleId != null) {
            Circle circle = circleService.getPublic(circleId);
            if (topicId != null && !topicId.equals(circle.getTopicId())) {
                throw new ConflictException("Circle 与 Topic 不匹配");
            }
            return new Association(circle.getTopicId(), circle.getId());
        }
        if (topicId != null) {
            Topic topic = catalogService.getTopic(topicId, false);
            return new Association(topic.getId(), null);
        }
        return new Association(null, null);
    }

    private void validateAssociation(Long topicId, Long circleId) {
        resolveAssociation(topicId, circleId);
    }

    private boolean associationsArePublic(Post post) {
        try {
            validateAssociation(post.getTopicId(), post.getCircleId());
            return true;
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }

    private void ensureAuthorEligible(long authorId) {
        if (accountService.getById(authorId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("账户状态不允许发布帖子");
        }
        IdentityVerification identity = identityService.findByAccountId(authorId);
        if (identity == null || identity.getStatus() != IdentityStatus.VERIFIED) {
            throw new ForbiddenException("完成实名认证后才能发布帖子");
        }
        if (!agePolicy.isAdult(identity.getBirthDate())) {
            throw new ForbiddenException("未满 18 周岁不能发布帖子");
        }
    }

    private Post requirePost(long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new ResourceNotFoundException("帖子不存在");
        }
        return post;
    }

    private Post requireOwnedPost(long authorId, long postId) {
        Post post = requirePost(postId);
        if (!post.getAuthorAccountId().equals(authorId)) {
            throw new ForbiddenException("只能操作自己发布的帖子");
        }
        return post;
    }

    private void persist(Post post) {
        if (postMapper.updateById(post) != 1) {
            throw new ConflictException("帖子已发生变化，请刷新后重试");
        }
    }

    private void audit(long postId, long operatorId, PostAuditAction action, PostStatus from, PostStatus to,
            String reason, LocalDateTime now) {
        auditMapper.insert(PostAuditLog.builder()
                .postId(postId)
                .operatorAccountId(operatorId)
                .action(action)
                .fromStatus(from)
                .toStatus(to)
                .reason(reason)
                .occurredAt(now)
                .build());
    }

    private record Association(Long topicId, Long circleId) {
    }
}
