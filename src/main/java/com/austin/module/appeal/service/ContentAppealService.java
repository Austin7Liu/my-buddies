package com.austin.module.appeal.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.appeal.domain.ContentAppeal;
import com.austin.module.appeal.domain.ContentAppealAuditAction;
import com.austin.module.appeal.domain.ContentAppealAuditLog;
import com.austin.module.appeal.domain.ContentAppealStatus;
import com.austin.module.appeal.mapper.ContentAppealAuditLogMapper;
import com.austin.module.appeal.mapper.ContentAppealMapper;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.post.service.PostCommentService;
import com.austin.module.post.service.PostService;
import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
import com.austin.module.report.mapper.ContentReportMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentAppealService {

    private final ContentAppealMapper appealMapper;
    private final ContentAppealAuditLogMapper auditMapper;
    private final ContentReportMapper reportMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostService postService;
    private final PostCommentService commentService;
    private final UserAccountService accountService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public ContentAppeal create(long appellantId, long reportId, String reason) {
        requireActive(appellantId);
        ContentReport report = requireResolvedReport(reportId);
        if (targetAuthor(report) != appellantId) {
            throw new ForbiddenException("只有被处置内容的作者可以申诉");
        }
        requireTargetStillActioned(report);
        LocalDateTime now = LocalDateTime.now(clock);
        ContentAppeal appeal = ContentAppeal.builder().reportId(reportId)
                .appellantAccountId(appellantId).reason(reason.trim())
                .status(ContentAppealStatus.PENDING).version(0)
                .createdAt(now).updatedAt(now).build();
        try {
            appealMapper.insert(appeal);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("该举报已经提交过申诉", exception);
        }
        return appeal;
    }

    @Transactional(readOnly = true)
    public IPage<ContentAppeal> listMine(long accountId, long page, long size) {
        return appealMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<ContentAppeal>()
                .eq(ContentAppeal::getAppellantAccountId, accountId)
                .orderByDesc(ContentAppeal::getCreatedAt).orderByDesc(ContentAppeal::getId));
    }

    @Transactional(readOnly = true)
    public ContentAppeal getMine(long accountId, long appealId) {
        ContentAppeal appeal = requireAppeal(appealId);
        if (appeal.getAppellantAccountId() != accountId) {
            throw new ResourceNotFoundException("申诉不存在");
        }
        return appeal;
    }

    @Transactional(readOnly = true)
    public IPage<ContentAppeal> listForAdmin(ContentAppealStatus status, long page, long size) {
        LambdaQueryWrapper<ContentAppeal> query = new LambdaQueryWrapper<ContentAppeal>()
                .orderByAsc(ContentAppeal::getCreatedAt).orderByAsc(ContentAppeal::getId);
        if (status != null) {
            query.eq(ContentAppeal::getStatus, status);
        }
        return appealMapper.selectPage(new Page<>(page, size), query);
    }

    @Transactional(readOnly = true)
    public ContentAppeal getForAdmin(long appealId) {
        return requireAppeal(appealId);
    }

    @Transactional
    public ContentAppeal approve(long operatorId, long appealId, String note) {
        ContentAppeal appeal = requirePending(appealId);
        ContentReport report = requireResolvedReport(appeal.getReportId());
        requireTargetStillActioned(report);
        if (report.getTargetType() == ReportTargetType.POST) {
            postService.restore(operatorId, report.getReportedPostId());
        } else {
            commentService.restore(operatorId, report.getReportedCommentId());
        }
        return handle(operatorId, appeal, ContentAppealStatus.APPROVED,
                ContentAppealAuditAction.APPROVE, note, NotificationType.CONTENT_APPEAL_APPROVED,
                "内容申诉已通过");
    }

    @Transactional
    public ContentAppeal reject(long operatorId, long appealId, String note) {
        return handle(operatorId, requirePending(appealId), ContentAppealStatus.REJECTED,
                ContentAppealAuditAction.REJECT, note, NotificationType.CONTENT_APPEAL_REJECTED,
                "内容申诉未通过");
    }

    @Transactional
    public ContentAppeal close(long operatorId, long appealId, String note) {
        return handle(operatorId, requirePending(appealId), ContentAppealStatus.CLOSED,
                ContentAppealAuditAction.CLOSE, note, NotificationType.CONTENT_APPEAL_CLOSED,
                "内容申诉已关闭");
    }

    private ContentAppeal handle(long operatorId, ContentAppeal appeal, ContentAppealStatus target,
            ContentAppealAuditAction action, String note, NotificationType type, String title) {
        LocalDateTime now = LocalDateTime.now(clock);
        appeal.setStatus(target);
        appeal.setReviewedBy(operatorId);
        appeal.setReviewedAt(now);
        appeal.setReviewNote(note.trim());
        appeal.setUpdatedAt(now);
        if (appealMapper.updateById(appeal) != 1) {
            throw new ConflictException("申诉状态已发生变化，请刷新后重试");
        }
        auditMapper.insert(ContentAppealAuditLog.builder().appealId(appeal.getId())
                .operatorAccountId(operatorId).action(action).fromStatus(ContentAppealStatus.PENDING)
                .toStatus(target).note(note.trim()).occurredAt(now).build());
        notificationService.notify(appeal.getAppellantAccountId(), type, title, note.trim(),
                NotificationReferenceType.CONTENT_APPEAL, appeal.getId(),
                "content-appeal:" + appeal.getId() + ":" + target);
        return requireAppeal(appeal.getId());
    }

    private long targetAuthor(ContentReport report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            Post post = postMapper.selectById(report.getReportedPostId());
            if (post == null) {
                throw new ResourceNotFoundException("被举报帖子不存在");
            }
            return post.getAuthorAccountId();
        }
        PostComment comment = commentMapper.selectById(report.getReportedCommentId());
        if (comment == null) {
            throw new ResourceNotFoundException("被举报评论不存在");
        }
        return comment.getAuthorAccountId();
    }

    private void requireTargetStillActioned(ContentReport report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            Post post = postMapper.selectById(report.getReportedPostId());
            if (post == null || post.getStatus() != PostStatus.OFFLINE) {
                throw new ConflictException("帖子当前状态无法申诉恢复");
            }
        } else {
            PostComment comment = commentMapper.selectById(report.getReportedCommentId());
            if (comment == null || comment.getStatus() != PostCommentStatus.HIDDEN_BY_ADMIN) {
                throw new ConflictException("评论当前状态无法申诉恢复");
            }
        }
    }

    private ContentReport requireResolvedReport(long reportId) {
        ContentReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new ResourceNotFoundException("举报不存在");
        }
        if (report.getStatus() != ReportStatus.RESOLVED) {
            throw new ConflictException("只有已确认违规的举报可以申诉");
        }
        return report;
    }

    private ContentAppeal requirePending(long appealId) {
        ContentAppeal appeal = requireAppeal(appealId);
        if (appeal.getStatus() != ContentAppealStatus.PENDING) {
            throw new ConflictException("只有待处理申诉可以复核");
        }
        return appeal;
    }

    private ContentAppeal requireAppeal(long appealId) {
        ContentAppeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new ResourceNotFoundException("申诉不存在");
        }
        return appeal;
    }

    private void requireActive(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("当前账户状态不允许申诉");
        }
    }
}
