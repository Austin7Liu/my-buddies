package com.austin.module.circle.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleAuditAction;
import com.austin.module.circle.domain.CircleAuditLog;
import com.austin.module.circle.domain.CircleStatus;
import com.austin.module.circle.mapper.CircleAuditLogMapper;
import com.austin.module.circle.mapper.CircleMapper;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.identity.policy.AgeEligibilityPolicy;
import com.austin.module.identity.service.IdentityVerificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CircleServiceImpl implements CircleService {
    private final CircleMapper circleMapper;
    private final CircleAuditLogMapper auditMapper;
    private final UserAccountService accountService;
    private final IdentityVerificationService identityService;
    private final AgeEligibilityPolicy agePolicy;
    private final CatalogService catalogService;
    private final Clock clock;

    @Override @Transactional(readOnly = true)
    public IPage<Circle> listPublic(long topicId, long page, long size) {
        catalogService.getTopic(topicId, false);
        return circleMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Circle>()
                .eq(Circle::getTopicId, topicId).eq(Circle::getStatus, CircleStatus.APPROVED)
                .orderByDesc(Circle::getCreatedAt).orderByDesc(Circle::getId));
    }

    @Override @Transactional(readOnly = true)
    public Circle getPublic(long circleId) {
        Circle value = requireCircle(circleId);
        if (value.getStatus() != CircleStatus.APPROVED) throw new ResourceNotFoundException("圈子不存在");
        catalogService.getTopic(value.getTopicId(), false);
        return value;
    }

    @Override @Transactional(readOnly = true)
    public IPage<Circle> listMine(long creatorId, long page, long size) {
        return circleMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Circle>()
                .eq(Circle::getCreatorAccountId, creatorId)
                .orderByDesc(Circle::getCreatedAt).orderByDesc(Circle::getId));
    }

    @Override @Transactional(readOnly = true)
    public IPage<Circle> listForReview(CircleStatus status, long page, long size) {
        LambdaQueryWrapper<Circle> query = new LambdaQueryWrapper<Circle>()
                .orderByAsc(Circle::getCreatedAt).orderByAsc(Circle::getId);
        if (status != null) query.eq(Circle::getStatus, status);
        return circleMapper.selectPage(new Page<>(page, size), query);
    }

    @Override @Transactional
    public Circle create(long creatorId, long topicId, String name, String description, String city, String district) {
        ensureCreatorEligible(creatorId); catalogService.getTopic(topicId, false);
        LocalDateTime now = LocalDateTime.now(clock);
        Circle value = Circle.builder().topicId(topicId).creatorAccountId(creatorId).name(name.trim())
                .description(trim(description)).city(city.trim()).district(trim(district))
                .status(CircleStatus.PENDING_REVIEW).version(0).createdAt(now).updatedAt(now).build();
        try { circleMapper.insert(value); }
        catch (DuplicateKeyException ex) { throw new ConflictException("该话题下已存在同名圈子", ex); }
        audit(value.getId(), creatorId, CircleAuditAction.SUBMIT, null, CircleStatus.PENDING_REVIEW, null, now);
        return value;
    }

    @Override @Transactional
    public Circle update(long creatorId, long circleId, String name, String description, String city, String district) {
        Circle value = requireCircle(circleId);
        if (!value.getCreatorAccountId().equals(creatorId)) throw new ForbiddenException("只能修改自己创建的圈子");
        if (!EnumSet.of(CircleStatus.PENDING_REVIEW, CircleStatus.REJECTED).contains(value.getStatus()))
            throw new ConflictException("当前圈子状态不允许修改");
        ensureCreatorEligible(creatorId); catalogService.getTopic(value.getTopicId(), false);
        CircleStatus from = value.getStatus(); LocalDateTime now = LocalDateTime.now(clock);
        value.setName(name.trim()); value.setDescription(trim(description)); value.setCity(city.trim()); value.setDistrict(trim(district));
        value.setStatus(CircleStatus.PENDING_REVIEW); value.setRejectionReason(null); value.setReviewedBy(null); value.setReviewedAt(null); value.setUpdatedAt(now);
        persist(value); audit(circleId, creatorId, from == CircleStatus.REJECTED ? CircleAuditAction.RESUBMIT : CircleAuditAction.UPDATE,
                from, CircleStatus.PENDING_REVIEW, null, now); return value;
    }

    @Override @Transactional
    public Circle approve(long reviewerId, long circleId) { return review(reviewerId, circleId, true, null); }

    @Override @Transactional
    public Circle reject(long reviewerId, long circleId, String reason) { return review(reviewerId, circleId, false, reason.trim()); }

    @Override @Transactional
    public Circle setEnabled(long reviewerId, long circleId, boolean enabled) {
        Circle value = requireCircle(circleId); CircleStatus from = value.getStatus();
        CircleStatus target = enabled ? CircleStatus.APPROVED : CircleStatus.DISABLED;
        if ((enabled && from != CircleStatus.DISABLED) || (!enabled && from != CircleStatus.APPROVED))
            throw new ConflictException("当前圈子状态不允许此操作");
        if (enabled) catalogService.getTopic(value.getTopicId(), false);
        LocalDateTime now = LocalDateTime.now(clock); value.setStatus(target); value.setReviewedBy(reviewerId);
        value.setReviewedAt(now); value.setRejectionReason(null); value.setUpdatedAt(now); persist(value);
        audit(circleId, reviewerId, enabled ? CircleAuditAction.RESTORE : CircleAuditAction.DISABLE, from, target, null, now); return value;
    }

    private Circle review(long reviewerId, long circleId, boolean approved, String reason) {
        Circle value = requireCircle(circleId);
        if (value.getStatus() != CircleStatus.PENDING_REVIEW) throw new ConflictException("圈子不处于待审核状态");
        if (approved) catalogService.getTopic(value.getTopicId(), false);
        LocalDateTime now = LocalDateTime.now(clock); CircleStatus target = approved ? CircleStatus.APPROVED : CircleStatus.REJECTED;
        value.setStatus(target); value.setRejectionReason(approved ? null : reason); value.setReviewedBy(reviewerId);
        value.setReviewedAt(now); value.setUpdatedAt(now); persist(value);
        audit(circleId, reviewerId, approved ? CircleAuditAction.APPROVE : CircleAuditAction.REJECT,
                CircleStatus.PENDING_REVIEW, target, reason, now); return value;
    }

    private void ensureCreatorEligible(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE)
            throw new ForbiddenException("账户状态不允许创建圈子");
        IdentityVerification identity = identityService.findByAccountId(accountId);
        if (identity == null || identity.getStatus() != IdentityStatus.VERIFIED)
            throw new ForbiddenException("完成实名认证后才能创建圈子");
        if (!agePolicy.isAdult(identity.getBirthDate())) throw new ForbiddenException("未满 18 周岁不能创建圈子");
    }

    private Circle requireCircle(long id) { Circle v = circleMapper.selectById(id); if (v == null) throw new ResourceNotFoundException("圈子不存在"); return v; }
    private void persist(Circle value) { try { if (circleMapper.updateById(value) != 1) throw new ConflictException("圈子已发生变化，请刷新后重试"); } catch (DuplicateKeyException ex) { throw new ConflictException("该话题下已存在同名圈子", ex); } }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void audit(long circleId, long operatorId, CircleAuditAction action, CircleStatus from, CircleStatus to, String reason, LocalDateTime now) {
        auditMapper.insert(CircleAuditLog.builder().circleId(circleId).operatorAccountId(operatorId).action(action)
                .fromStatus(from).toStatus(to).reason(reason).occurredAt(now).build());
    }
}

