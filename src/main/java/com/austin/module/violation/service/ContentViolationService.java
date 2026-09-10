package com.austin.module.violation.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.appeal.domain.ContentAppeal;
import com.austin.module.appeal.domain.ContentAppealStatus;
import com.austin.module.appeal.mapper.ContentAppealMapper;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
import com.austin.module.report.mapper.ContentReportMapper;
import com.austin.module.risk.domain.AccountBusinessRestriction;
import com.austin.module.risk.domain.RestrictionType;
import com.austin.module.risk.domain.RestrictionStatus;
import com.austin.module.risk.service.RiskRestrictionService;
import com.austin.module.violation.domain.AccountContentViolation;
import com.austin.module.violation.domain.AccountContentViolationAuditLog;
import com.austin.module.violation.domain.ContentViolationRestriction;
import com.austin.module.violation.domain.ViolationAuditAction;
import com.austin.module.violation.domain.ViolationPenaltyType;
import com.austin.module.violation.domain.ViolationSeverity;
import com.austin.module.violation.domain.ViolationStatus;
import com.austin.module.violation.mapper.AccountContentViolationAuditLogMapper;
import com.austin.module.violation.mapper.AccountContentViolationMapper;
import com.austin.module.violation.mapper.ContentViolationRestrictionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentViolationService {
    private final AccountContentViolationMapper violationMapper;
    private final AccountContentViolationAuditLogMapper auditMapper;
    private final ContentViolationRestrictionMapper linkMapper;
    private final ContentReportMapper reportMapper;
    private final ContentAppealMapper appealMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final RiskRestrictionService restrictionService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public ViolationView confirm(long operatorId, long reportId, ViolationSeverity severity,
            ViolationPenaltyType penaltyType, LocalDateTime expiresAt, String note) {
        ContentReport report = requireFinalReport(reportId);
        long accountId = targetAuthor(report);
        LocalDateTime now = LocalDateTime.now(clock);
        validatePenalty(penaltyType, expiresAt, now);
        AccountContentViolation violation = AccountContentViolation.builder().accountId(accountId)
                .reportId(reportId).severity(severity).penaltyType(penaltyType)
                .penaltyExpiresAt(expiresAt).note(note.trim()).status(ViolationStatus.ACTIVE)
                .confirmedBy(operatorId).confirmedAt(now).version(0).createdAt(now).updatedAt(now).build();
        try {
            violationMapper.insert(violation);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("该举报已经生成违规记录", exception);
        }
        if (penaltyType == ViolationPenaltyType.POST_DISABLED
                || penaltyType == ViolationPenaltyType.CONTENT_CREATE_DISABLED) {
            link(violation.getId(), restrictionService.create(operatorId, accountId,
                    RestrictionType.POST_CREATE_DISABLED, note, expiresAt), now);
        }
        if (penaltyType == ViolationPenaltyType.COMMENT_DISABLED
                || penaltyType == ViolationPenaltyType.CONTENT_CREATE_DISABLED) {
            link(violation.getId(), restrictionService.create(operatorId, accountId,
                    RestrictionType.COMMENT_CREATE_DISABLED, note, expiresAt), now);
        }
        auditMapper.insert(AccountContentViolationAuditLog.builder().violationId(violation.getId())
                .operatorAccountId(operatorId).action(ViolationAuditAction.CONFIRM)
                .reason(note.trim()).occurredAt(now).build());
        notificationService.notify(accountId, NotificationType.CONTENT_VIOLATION_CONFIRMED,
                "内容违规记录已确认", note.trim(), NotificationReferenceType.CONTENT_VIOLATION,
                violation.getId(), "content-violation:" + violation.getId() + ":confirmed");
        return view(violation);
    }

    @Transactional
    public ViolationView revoke(long operatorId, long violationId, String reason,
            boolean revokeLinkedRestrictions) {
        AccountContentViolation violation = requireViolation(violationId);
        if (violation.getStatus() != ViolationStatus.ACTIVE) {
            throw new ConflictException("只有生效中的违规记录可以撤销");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        violation.setStatus(ViolationStatus.REVOKED);
        violation.setRevokedBy(operatorId);
        violation.setRevokedAt(now);
        violation.setRevokeReason(reason.trim());
        violation.setUpdatedAt(now);
        if (violationMapper.updateById(violation) != 1) {
            throw new ConflictException("违规记录已发生变化，请刷新后重试");
        }
        if (revokeLinkedRestrictions) {
            for (ContentViolationRestriction link : links(violationId)) {
                var restriction = restrictionService.getById(link.getRestrictionId());
                if (restriction.getStatus() == RestrictionStatus.ACTIVE) {
                    restrictionService.revoke(operatorId, restriction.getId(), reason);
                }
            }
        }
        auditMapper.insert(AccountContentViolationAuditLog.builder().violationId(violationId)
                .operatorAccountId(operatorId).action(ViolationAuditAction.REVOKE)
                .reason(reason.trim()).occurredAt(now).build());
        notificationService.notify(violation.getAccountId(), NotificationType.CONTENT_VIOLATION_REVOKED,
                "内容违规记录已撤销", reason.trim(), NotificationReferenceType.CONTENT_VIOLATION,
                violationId, "content-violation:" + violationId + ":revoked");
        return view(requireViolation(violationId));
    }

    @Transactional(readOnly = true)
    public IPage<AccountContentViolation> listMine(long accountId, long page, long size) {
        return violationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AccountContentViolation>()
                        .eq(AccountContentViolation::getAccountId, accountId)
                        .orderByDesc(AccountContentViolation::getCreatedAt));
    }

    @Transactional(readOnly = true)
    public IPage<AccountContentViolation> listForAdmin(long accountId, long page, long size) {
        return listMine(accountId, page, size);
    }

    @Transactional(readOnly = true)
    public ViolationView getMine(long accountId, long violationId) {
        AccountContentViolation violation = requireViolation(violationId);
        if (violation.getAccountId() != accountId) {
            throw new ResourceNotFoundException("违规记录不存在");
        }
        return view(violation);
    }

    @Transactional(readOnly = true)
    public ViolationView getForAdmin(long violationId) {
        return view(requireViolation(violationId));
    }

    private ContentReport requireFinalReport(long reportId) {
        ContentReport report = reportMapper.selectById(reportId);
        if (report == null || report.getStatus() != ReportStatus.RESOLVED) {
            throw new ConflictException("举报尚未形成有效违规结论");
        }
        ContentAppeal appeal = appealMapper.selectOne(new LambdaQueryWrapper<ContentAppeal>()
                .eq(ContentAppeal::getReportId, reportId));
        if (appeal != null && appeal.getStatus() != ContentAppealStatus.REJECTED) {
            throw new ConflictException("申诉尚未形成可处罚结论");
        }
        if (appeal == null && (report.getAppealDeadlineAt() == null
                || !LocalDateTime.now(clock).isAfter(report.getAppealDeadlineAt()))) {
            throw new ConflictException("举报仍处于申诉期");
        }
        return report;
    }

    private long targetAuthor(ContentReport report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            Post post = postMapper.selectById(report.getReportedPostId());
            if (post == null) {
                throw new ResourceNotFoundException("帖子不存在");
            }
            return post.getAuthorAccountId();
        }
        PostComment comment = commentMapper.selectById(report.getReportedCommentId());
        if (comment == null) {
            throw new ResourceNotFoundException("评论不存在");
        }
        return comment.getAuthorAccountId();
    }

    private AccountContentViolation requireViolation(long id) {
        AccountContentViolation violation = violationMapper.selectById(id);
        if (violation == null) {
            throw new ResourceNotFoundException("违规记录不存在");
        }
        return violation;
    }

    private void validatePenalty(ViolationPenaltyType type, LocalDateTime expiresAt,
            LocalDateTime now) {
        if (type == ViolationPenaltyType.WARNING_ONLY && expiresAt != null) {
            throw new ConflictException("仅警告时不能填写限制到期时间");
        }
        if (type != ViolationPenaltyType.WARNING_ONLY && expiresAt == null) {
            throw new ConflictException("业务限制必须填写到期时间");
        }
        if (expiresAt != null
                && (expiresAt.isBefore(now.plusDays(1)) || expiresAt.isAfter(now.plusDays(30)))) {
            throw new ConflictException("限制期限必须在 1 到 30 天之间");
        }
    }

    private void link(long violationId, AccountBusinessRestriction restriction, LocalDateTime now) {
        linkMapper.insert(ContentViolationRestriction.builder()
                .violationId(violationId)
                .restrictionId(restriction.getId())
                .restrictionType(restriction.getRestrictionType())
                .createdAt(now)
                .build());
    }

    private List<ContentViolationRestriction> links(long violationId) {
        return linkMapper.selectList(new LambdaQueryWrapper<ContentViolationRestriction>()
                .eq(ContentViolationRestriction::getViolationId, violationId));
    }

    private ViolationView view(AccountContentViolation violation) {
        return new ViolationView(violation,
                links(violation.getId()).stream().map(ContentViolationRestriction::getRestrictionId).toList());
    }

    public record ViolationView(
            AccountContentViolation violation,
            List<Long> linkedRestrictionIds) {
    }
}
