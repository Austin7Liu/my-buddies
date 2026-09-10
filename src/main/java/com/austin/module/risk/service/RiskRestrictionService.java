package com.austin.module.risk.service;

import com.austin.common.exception.BusinessRestrictedException;
import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.mapper.UserAccountMapper;
import com.austin.module.risk.domain.AccountBusinessRestriction;
import com.austin.module.risk.domain.AccountBusinessRestrictionAuditLog;
import com.austin.module.risk.domain.RestrictionAuditAction;
import com.austin.module.risk.domain.RestrictionStatus;
import com.austin.module.risk.domain.RestrictionType;
import com.austin.module.risk.mapper.AccountBusinessRestrictionAuditLogMapper;
import com.austin.module.risk.mapper.AccountBusinessRestrictionMapper;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
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
public class RiskRestrictionService {

    private final AccountBusinessRestrictionMapper restrictionMapper;
    private final AccountBusinessRestrictionAuditLogMapper auditMapper;
    private final UserAccountMapper accountMapper;
    private final Clock clock;
    private final NotificationService notificationService;

    @Transactional
    public AccountBusinessRestriction create(long operatorId, long accountId, RestrictionType type,
            String reason, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateReason(reason, "新增业务限制必须填写原因");
        validatePeriod(now, expiresAt);
        UserAccount account = accountMapper.selectByIdForUpdate(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("账户不存在");
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("只有正常账户可以新增业务限制");
        }
        expireDue(accountId, type, now);
        AccountBusinessRestriction restriction = AccountBusinessRestriction.builder()
                .accountId(accountId)
                .restrictionType(type)
                .status(RestrictionStatus.ACTIVE)
                .reason(reason.trim())
                .startsAt(now)
                .expiresAt(expiresAt)
                .createdBy(operatorId)
                .activeMarker(1)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            restrictionMapper.insert(restriction);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("该账户已存在同类型有效限制", exception);
        }
        audit(restriction.getId(), operatorId, RestrictionAuditAction.CREATE, reason.trim(), now);
        notificationService.notify(accountId, NotificationType.RISK_RESTRICTION_CREATED,
                "账户业务能力暂时受限", "限制原因：" + reason.trim(),
                NotificationReferenceType.RISK_RESTRICTION, restriction.getId(),
                "restriction:" + restriction.getId() + ":created");
        return restriction;
    }

    @Transactional
    public AccountBusinessRestriction revoke(long operatorId, long restrictionId, String reason) {
        validateReason(reason, "撤销业务限制必须填写原因");
        AccountBusinessRestriction initial = requireRestriction(restrictionId);
        accountMapper.selectByIdForUpdate(initial.getAccountId());
        LocalDateTime now = LocalDateTime.now(clock);
        expireDue(initial.getAccountId(), initial.getRestrictionType(), now);
        AccountBusinessRestriction restriction = requireRestriction(restrictionId);
        if (restriction.getStatus() != RestrictionStatus.ACTIVE) {
            throw new ConflictException("只有生效中的限制可以撤销");
        }
        restriction.setStatus(RestrictionStatus.REVOKED);
        restriction.setRevokedAt(now);
        restriction.setRevokedBy(operatorId);
        restriction.setRevokeReason(reason.trim());
        restriction.setActiveMarker(null);
        restriction.setUpdatedAt(now);
        persist(restriction);
        audit(restrictionId, operatorId, RestrictionAuditAction.REVOKE, reason.trim(), now);
        notificationService.notify(restriction.getAccountId(), NotificationType.RISK_RESTRICTION_REVOKED,
                "账户业务限制已撤销", "撤销原因：" + reason.trim(),
                NotificationReferenceType.RISK_RESTRICTION, restrictionId,
                "restriction:" + restrictionId + ":revoked");
        return restriction;
    }

    @Transactional(readOnly = true)
    public AccountBusinessRestriction getById(long restrictionId) {
        return requireRestriction(restrictionId);
    }

    @Transactional
    public void ensureAllowed(long accountId, RestrictionType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        expireDue(accountId, type, now);
        long activeCount = restrictionMapper.selectCount(new LambdaQueryWrapper<AccountBusinessRestriction>()
                .eq(AccountBusinessRestriction::getAccountId, accountId)
                .eq(AccountBusinessRestriction::getRestrictionType, type)
                .eq(AccountBusinessRestriction::getStatus, RestrictionStatus.ACTIVE)
                .le(AccountBusinessRestriction::getStartsAt, now)
                .gt(AccountBusinessRestriction::getExpiresAt, now));
        if (activeCount > 0) {
            throw new BusinessRestrictedException(restrictionMessage(type));
        }
    }

    private String restrictionMessage(RestrictionType type) {
        return switch (type) {
            case MEETUP_CREATE_DISABLED -> "当前账户暂时不能创建活动";
            case MEETUP_JOIN_DISABLED -> "当前账户暂时不能申请加入活动";
            case POST_CREATE_DISABLED -> "当前账户暂时不能发布帖子";
            case COMMENT_CREATE_DISABLED -> "当前账户暂时不能发表评论";
        };
    }

    @Transactional
    public IPage<AccountBusinessRestriction> listMine(long accountId, long page, long size) {
        expireDue(accountId, null, LocalDateTime.now(clock));
        return list(accountId, page, size);
    }

    @Transactional
    public IPage<AccountBusinessRestriction> listForAdmin(long accountId, long page, long size) {
        if (accountMapper.selectById(accountId) == null) {
            throw new ResourceNotFoundException("账户不存在");
        }
        expireDue(accountId, null, LocalDateTime.now(clock));
        return list(accountId, page, size);
    }

    private void expireDue(long accountId, RestrictionType type, LocalDateTime now) {
        LambdaQueryWrapper<AccountBusinessRestriction> query = new LambdaQueryWrapper<AccountBusinessRestriction>()
                .eq(AccountBusinessRestriction::getAccountId, accountId)
                .eq(AccountBusinessRestriction::getStatus, RestrictionStatus.ACTIVE)
                .le(AccountBusinessRestriction::getExpiresAt, now);
        if (type != null) {
            query.eq(AccountBusinessRestriction::getRestrictionType, type);
        }
        List<AccountBusinessRestriction> expired = restrictionMapper.selectList(query);
        for (AccountBusinessRestriction restriction : expired) {
            restriction.setStatus(RestrictionStatus.EXPIRED);
            restriction.setActiveMarker(null);
            restriction.setUpdatedAt(now);
            persist(restriction);
            audit(restriction.getId(), null, RestrictionAuditAction.EXPIRE, null, now);
        }
    }

    private IPage<AccountBusinessRestriction> list(long accountId, long page, long size) {
        return restrictionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AccountBusinessRestriction>()
                        .eq(AccountBusinessRestriction::getAccountId, accountId)
                        .orderByDesc(AccountBusinessRestriction::getCreatedAt)
                        .orderByDesc(AccountBusinessRestriction::getId));
    }

    private AccountBusinessRestriction requireRestriction(long restrictionId) {
        AccountBusinessRestriction restriction = restrictionMapper.selectById(restrictionId);
        if (restriction == null) {
            throw new ResourceNotFoundException("业务限制不存在");
        }
        return restriction;
    }

    private void persist(AccountBusinessRestriction restriction) {
        if (restrictionMapper.updateById(restriction) != 1) {
            throw new ConflictException("业务限制已发生变化，请刷新后重试");
        }
    }

    private void audit(long restrictionId, Long operatorId, RestrictionAuditAction action,
            String reason, LocalDateTime now) {
        auditMapper.insert(AccountBusinessRestrictionAuditLog.builder()
                .restrictionId(restrictionId)
                .operatorAccountId(operatorId)
                .action(action)
                .reason(reason)
                .occurredAt(now)
                .build());
    }

    private void validatePeriod(LocalDateTime now, LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new ConflictException("必须填写限制到期时间");
        }
        if (expiresAt.isBefore(now.plusDays(1)) || expiresAt.isAfter(now.plusDays(30))) {
            throw new ConflictException("限制期限必须在 1 到 30 天之间");
        }
    }

    private void validateReason(String reason, String message) {
        if (reason == null || reason.isBlank()) {
            throw new ConflictException(message);
        }
    }
}
