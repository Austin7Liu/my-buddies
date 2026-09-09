package com.austin.module.post.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.identity.policy.AgeEligibilityPolicy;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentAuditAction;
import com.austin.module.post.domain.PostCommentAuditLog;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.post.mapper.PostCommentAuditLogMapper;
import com.austin.module.post.mapper.PostCommentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentMapper commentMapper;
    private final PostCommentAuditLogMapper auditLogMapper;
    private final PostService postService;
    private final UserAccountService accountService;
    private final IdentityVerificationService identityService;
    private final AgeEligibilityPolicy ageEligibilityPolicy;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public IPage<PostComment> list(long postId, long page, long size) {
        postService.getPublic(postId);
        return commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .orderByAsc(PostComment::getCreatedAt)
                        .orderByAsc(PostComment::getId));
    }

    @Transactional
    public PostComment create(long accountId, long postId, Long parentCommentId, String content) {
        requireEligible(accountId);
        Post post = postService.getPublic(postId);
        PostComment parent = parentCommentId == null ? null : requireComment(parentCommentId);
        if (parent != null && (!parent.getPostId().equals(postId)
                || parent.getParentCommentId() != null
                || parent.getStatus() != PostCommentStatus.VISIBLE)) {
            throw new ConflictException("只能回复该帖子的可见一级评论");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PostComment comment = PostComment.builder()
                .postId(postId)
                .authorAccountId(accountId)
                .parentCommentId(parentCommentId)
                .content(content.trim())
                .status(PostCommentStatus.VISIBLE)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        commentMapper.insert(comment);
        notifyRecipient(accountId, post, parent, comment);
        return comment;
    }

    @Transactional
    public PostComment update(long accountId, long postId, long commentId, String content) {
        PostComment comment = requireOwned(accountId, postId, commentId);
        if (comment.getStatus() != PostCommentStatus.VISIBLE) {
            throw new ConflictException("只有可见评论可以编辑");
        }
        comment.setContent(content.trim());
        comment.setUpdatedAt(LocalDateTime.now(clock));
        requireOne(commentMapper.updateById(comment));
        return requireComment(commentId);
    }

    @Transactional
    public PostComment delete(long accountId, long postId, long commentId) {
        PostComment comment = requireOwned(accountId, postId, commentId);
        if (comment.getStatus() != PostCommentStatus.VISIBLE) {
            throw new ConflictException("评论已不可删除");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        comment.setStatus(PostCommentStatus.DELETED_BY_AUTHOR);
        comment.setDeletedAt(now);
        comment.setUpdatedAt(now);
        requireOne(commentMapper.updateById(comment));
        return requireComment(commentId);
    }

    @Transactional
    public PostComment hide(long operatorId, long commentId, String reason) {
        PostComment comment = requireComment(commentId);
        if (comment.getStatus() != PostCommentStatus.VISIBLE) {
            throw new ConflictException("只有可见评论可以隐藏");
        }
        return moderate(operatorId, comment, PostCommentStatus.HIDDEN_BY_ADMIN,
                PostCommentAuditAction.HIDE, reason.trim());
    }

    @Transactional
    public PostComment restore(long operatorId, long commentId) {
        PostComment comment = requireComment(commentId);
        if (comment.getStatus() != PostCommentStatus.HIDDEN_BY_ADMIN) {
            throw new ConflictException("只有管理员隐藏的评论可以恢复");
        }
        return moderate(operatorId, comment, PostCommentStatus.VISIBLE,
                PostCommentAuditAction.RESTORE, null);
    }

    private PostComment moderate(long operatorId, PostComment comment, PostCommentStatus target,
            PostCommentAuditAction action, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        comment.setStatus(target);
        comment.setUpdatedAt(now);
        requireOne(commentMapper.updateById(comment));
        auditLogMapper.insert(PostCommentAuditLog.builder()
                .commentId(comment.getId())
                .operatorAccountId(operatorId)
                .action(action)
                .reason(reason)
                .occurredAt(now)
                .build());
        return requireComment(comment.getId());
    }

    private void notifyRecipient(long actorId, Post post, PostComment parent, PostComment comment) {
        long recipientId = parent == null ? post.getAuthorAccountId() : parent.getAuthorAccountId();
        if (recipientId == actorId) {
            return;
        }
        NotificationType type = parent == null
                ? NotificationType.POST_COMMENTED : NotificationType.COMMENT_REPLIED;
        String title = parent == null ? "帖子收到新评论" : "评论收到新回复";
        notificationService.notify(recipientId, type, title, "有人参与了你的讨论",
                NotificationReferenceType.POST_COMMENT, comment.getId(),
                "post-comment:" + comment.getId() + ":" + recipientId);
    }

    private void requireEligible(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("当前账户状态不允许评论");
        }
        var identity = identityService.findByAccountId(accountId);
        if (identity.getStatus() != IdentityStatus.VERIFIED
                || !ageEligibilityPolicy.isAdult(identity.getBirthDate())) {
            throw new ForbiddenException("完成成年人实名认证后才能评论");
        }
    }

    private PostComment requireOwned(long accountId, long postId, long commentId) {
        PostComment comment = requireComment(commentId);
        if (!comment.getPostId().equals(postId)) {
            throw new ResourceNotFoundException("评论不存在");
        }
        if (!comment.getAuthorAccountId().equals(accountId)) {
            throw new ForbiddenException("无权操作该评论");
        }
        return comment;
    }

    private PostComment requireComment(long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ResourceNotFoundException("评论不存在");
        }
        return comment;
    }

    private void requireOne(int affected) {
        if (affected != 1) {
            throw new ConflictException("评论已发生变化，请刷新后重试");
        }
    }
}
