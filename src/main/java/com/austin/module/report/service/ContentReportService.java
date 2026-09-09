package com.austin.module.report.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.post.service.PostCommentService;
import com.austin.module.post.service.PostService;
import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ContentReportAuditLog;
import com.austin.module.report.domain.ReportAuditAction;
import com.austin.module.report.domain.ReportReasonType;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
import com.austin.module.report.mapper.ContentReportAuditLogMapper;
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
public class ContentReportService {

    private final ContentReportMapper reportMapper;
    private final ContentReportAuditLogMapper auditMapper;
    private final PostCommentMapper commentMapper;
    private final PostService postService;
    private final PostCommentService commentService;
    private final UserAccountService accountService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public ContentReport reportPost(long reporterId, long postId, ReportReasonType reason,
            String description) {
        requireActive(reporterId);
        Post post = postService.getPublic(postId);
        if (post.getAuthorAccountId() == reporterId) {
            throw new ConflictException("不能举报自己发布的内容");
        }
        return create(reporterId, ReportTargetType.POST, postId, null, reason, description,
                post.getContent());
    }

    @Transactional
    public ContentReport reportComment(long reporterId, long commentId, ReportReasonType reason,
            String description) {
        requireActive(reporterId);
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != PostCommentStatus.VISIBLE) {
            throw new ResourceNotFoundException("评论不存在");
        }
        postService.getPublic(comment.getPostId());
        if (comment.getAuthorAccountId() == reporterId) {
            throw new ConflictException("不能举报自己发布的内容");
        }
        return create(reporterId, ReportTargetType.POST_COMMENT, null, commentId, reason,
                description, comment.getContent());
    }

    @Transactional(readOnly = true)
    public IPage<ContentReport> listMine(long reporterId, long page, long size) {
        return reportMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getReporterAccountId, reporterId)
                .orderByDesc(ContentReport::getCreatedAt)
                .orderByDesc(ContentReport::getId));
    }

    @Transactional(readOnly = true)
    public ContentReport getMine(long reporterId, long reportId) {
        ContentReport report = requireReport(reportId);
        if (report.getReporterAccountId() != reporterId) {
            throw new ResourceNotFoundException("举报不存在");
        }
        return report;
    }

    @Transactional(readOnly = true)
    public IPage<ContentReport> listForAdmin(ReportStatus status, long page, long size) {
        LambdaQueryWrapper<ContentReport> query = new LambdaQueryWrapper<ContentReport>()
                .orderByAsc(ContentReport::getCreatedAt).orderByAsc(ContentReport::getId);
        if (status != null) {
            query.eq(ContentReport::getStatus, status);
        }
        return reportMapper.selectPage(new Page<>(page, size), query);
    }

    @Transactional(readOnly = true)
    public ContentReport getForAdmin(long reportId) {
        return requireReport(reportId);
    }

    @Transactional
    public ContentReport resolve(long operatorId, long reportId, String note) {
        ContentReport report = requirePending(reportId);
        if (report.getTargetType() == ReportTargetType.POST) {
            postService.offline(operatorId, report.getReportedPostId(), note.trim());
        } else {
            commentService.hide(operatorId, report.getReportedCommentId(), note.trim());
        }
        return handle(operatorId, report, ReportStatus.RESOLVED, ReportAuditAction.RESOLVE,
                note.trim(), NotificationType.REPORT_RESOLVED, "举报已处理");
    }

    @Transactional
    public ContentReport reject(long operatorId, long reportId, String note) {
        return handle(operatorId, requirePending(reportId), ReportStatus.REJECTED,
                ReportAuditAction.REJECT, note.trim(), NotificationType.REPORT_REJECTED,
                "举报审核未通过");
    }

    @Transactional
    public ContentReport markDuplicate(long operatorId, long reportId, String note) {
        return handle(operatorId, requirePending(reportId), ReportStatus.DUPLICATE,
                ReportAuditAction.MARK_DUPLICATE, note.trim(), null, null);
    }

    private ContentReport create(long reporterId, ReportTargetType targetType, Long postId,
            Long commentId, ReportReasonType reason, String description, String snapshot) {
        if (reason == ReportReasonType.OTHER && (description == null || description.isBlank())) {
            throw new ConflictException("选择其他原因时必须填写说明");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ContentReport report = ContentReport.builder().reporterAccountId(reporterId)
                .targetType(targetType).reportedPostId(postId).reportedCommentId(commentId)
                .reasonType(reason).description(trim(description)).contentSnapshot(snapshot)
                .status(ReportStatus.PENDING).activeMarker(1).version(0)
                .createdAt(now).updatedAt(now).build();
        try {
            reportMapper.insert(report);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("你已经提交过相同内容的待处理举报", exception);
        }
        return report;
    }

    private ContentReport handle(long operatorId, ContentReport report, ReportStatus target,
            ReportAuditAction action, String note, NotificationType notificationType, String title) {
        LocalDateTime now = LocalDateTime.now(clock);
        report.setStatus(target);
        report.setHandledBy(operatorId);
        report.setHandledAt(now);
        report.setResolutionNote(note);
        report.setActiveMarker(null);
        report.setUpdatedAt(now);
        if (reportMapper.updateById(report) != 1) {
            throw new ConflictException("举报状态已发生变化，请刷新后重试");
        }
        auditMapper.insert(ContentReportAuditLog.builder().reportId(report.getId())
                .operatorAccountId(operatorId).action(action).fromStatus(ReportStatus.PENDING)
                .toStatus(target).note(note).occurredAt(now).build());
        if (notificationType != null) {
            notificationService.notify(report.getReporterAccountId(), notificationType, title,
                    note, NotificationReferenceType.CONTENT_REPORT, report.getId(),
                    "content-report:" + report.getId() + ":" + target);
        }
        return requireReport(report.getId());
    }

    private ContentReport requirePending(long reportId) {
        ContentReport report = requireReport(reportId);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ConflictException("只有待处理举报可以审核");
        }
        return report;
    }

    private ContentReport requireReport(long reportId) {
        ContentReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new ResourceNotFoundException("举报不存在");
        }
        return report;
    }

    private void requireActive(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("当前账户状态不允许举报");
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
