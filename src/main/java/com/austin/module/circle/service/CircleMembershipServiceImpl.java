package com.austin.module.circle.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleMember;
import com.austin.module.circle.domain.CircleMemberRole;
import com.austin.module.circle.domain.CircleMemberStatus;
import com.austin.module.circle.domain.CircleStatus;
import com.austin.module.circle.mapper.CircleMapper;
import com.austin.module.circle.mapper.CircleMemberMapper;
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
public class CircleMembershipServiceImpl implements CircleMembershipService {

    private final CircleMemberMapper memberMapper;
    private final CircleMapper circleMapper;
    private final CatalogService catalogService;
    private final UserAccountService accountService;
    private final Clock clock;

    @Override
    @Transactional
    public CircleMember join(long accountId, long circleId) {
        Circle circle = requirePublicCircle(circleId);
        ensureAccountActive(accountId);
        CircleMember existing = find(accountId, circle.getId());
        if (existing != null) {
            if (existing.getStatus() == CircleMemberStatus.ACTIVE) {
                return existing;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            existing.setStatus(CircleMemberStatus.ACTIVE);
            existing.setJoinedAt(now);
            existing.setLeftAt(null);
            existing.setUpdatedAt(now);
            persist(existing);
            return existing;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        CircleMember member = CircleMember.builder()
                .circleId(circle.getId())
                .accountId(accountId)
                .role(CircleMemberRole.MEMBER)
                .status(CircleMemberStatus.ACTIVE)
                .joinedAt(now)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            memberMapper.insert(member);
            return member;
        } catch (DuplicateKeyException exception) {
            CircleMember concurrent = find(accountId, circleId);
            if (concurrent != null && concurrent.getStatus() == CircleMemberStatus.ACTIVE) {
                return concurrent;
            }
            throw new ConflictException("圈子成员状态已发生变化，请刷新后重试", exception);
        }
    }

    @Override
    @Transactional
    public CircleMember leave(long accountId, long circleId) {
        requireCircle(circleId);
        CircleMember member = requireMembership(accountId, circleId);
        if (member.getRole() == CircleMemberRole.OWNER) {
            throw new ConflictException("圈子创建者不能退出自己的圈子");
        }
        if (member.getStatus() == CircleMemberStatus.LEFT) {
            return member;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        member.setStatus(CircleMemberStatus.LEFT);
        member.setLeftAt(now);
        member.setUpdatedAt(now);
        persist(member);
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public CircleMember getMine(long accountId, long circleId) {
        requireCircle(circleId);
        return requireMembership(accountId, circleId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<CircleMember> listMembers(long circleId, long page, long size) {
        requirePublicCircle(circleId);
        return memberMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getStatus, CircleMemberStatus.ACTIVE)
                .orderByAsc(CircleMember::getRole)
                .orderByAsc(CircleMember::getJoinedAt)
                .orderByAsc(CircleMember::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Circle> listMyCircles(long accountId, long page, long size) {
        return circleMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Circle>()
                .eq(Circle::getStatus, CircleStatus.APPROVED)
                .apply("EXISTS (SELECT 1 FROM circle_member cm WHERE cm.circle_id = circle.id "
                        + "AND cm.account_id = {0} AND cm.status = 'ACTIVE')", accountId)
                .orderByDesc(Circle::getCreatedAt)
                .orderByDesc(Circle::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureActiveMember(long accountId, long circleId) {
        CircleMember member = find(accountId, circleId);
        if (member == null || member.getStatus() != CircleMemberStatus.ACTIVE) {
            throw new ForbiddenException("加入圈子后才能在圈子内发帖");
        }
    }

    @Override
    public void createOwnerMembership(long accountId, long circleId, LocalDateTime now) {
        memberMapper.insert(CircleMember.builder()
                .circleId(circleId)
                .accountId(accountId)
                .role(CircleMemberRole.OWNER)
                .status(CircleMemberStatus.ACTIVE)
                .joinedAt(now)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void ensureAccountActive(long accountId) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("账户状态不允许加入圈子");
        }
    }

    private Circle requirePublicCircle(long circleId) {
        Circle circle = requireCircle(circleId);
        if (circle.getStatus() != CircleStatus.APPROVED) {
            throw new ResourceNotFoundException("圈子不存在");
        }
        catalogService.getTopic(circle.getTopicId(), false);
        return circle;
    }

    private Circle requireCircle(long circleId) {
        Circle circle = circleMapper.selectById(circleId);
        if (circle == null) {
            throw new ResourceNotFoundException("圈子不存在");
        }
        return circle;
    }

    private CircleMember requireMembership(long accountId, long circleId) {
        CircleMember member = find(accountId, circleId);
        if (member == null) {
            throw new ResourceNotFoundException("圈子成员关系不存在");
        }
        return member;
    }

    private CircleMember find(long accountId, long circleId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getAccountId, accountId));
    }

    private void persist(CircleMember member) {
        if (memberMapper.updateById(member) != 1) {
            throw new ConflictException("圈子成员状态已发生变化，请刷新后重试");
        }
    }
}
