package com.austin.module.meetup.service;

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
import com.austin.module.meetup.domain.GenderRequirement;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupAuditAction;
import com.austin.module.meetup.domain.MeetupAuditLog;
import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.meetup.domain.MeetupOnlineDetail;
import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.MeetupStatus;
import com.austin.module.meetup.domain.ParticipantRole;
import com.austin.module.meetup.domain.ParticipantStatus;
import com.austin.module.meetup.mapper.MeetupAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.meetup.mapper.MeetupOnlineDetailMapper;
import com.austin.module.meetup.mapper.MeetupParticipantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetupServiceImpl implements MeetupService {

    private static final List<MeetupStatus> PUBLIC_STATUSES = List.of(MeetupStatus.OPEN, MeetupStatus.CONFIRMED);

    private final MeetupMapper meetupMapper;
    private final MeetupOnlineDetailMapper onlineDetailMapper;
    private final MeetupParticipantMapper participantMapper;
    private final MeetupAuditLogMapper auditMapper;
    private final UserAccountService accountService;
    private final IdentityVerificationService identityService;
    private final AgeEligibilityPolicy agePolicy;
    private final CatalogService catalogService;
    private final CircleService circleService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public IPage<Meetup> listPublic(Long topicId, Long circleId, long page, long size) {
        if (circleId != null) {
            Circle circle = circleService.getPublic(circleId);
            if (topicId != null && !topicId.equals(circle.getTopicId())) {
                throw new ConflictException("Circle 与 Topic 不匹配");
            }
        } else if (topicId != null) {
            catalogService.getTopic(topicId, false);
        }
        LambdaQueryWrapper<Meetup> query = publicQuery();
        if (topicId != null) {
            query.eq(Meetup::getTopicId, topicId);
        }
        if (circleId != null) {
            query.eq(Meetup::getCircleId, circleId);
        }
        return meetupMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Meetup> listMine(long accountId, long page, long size) {
        return meetupMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Meetup>()
                .apply("EXISTS (SELECT 1 FROM meetup_participant mp WHERE mp.meetup_id = meetup.id "
                        + "AND mp.account_id = {0})", accountId)
                .orderByDesc(Meetup::getStartTime)
                .orderByDesc(Meetup::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<Meetup> listForAdmin(MeetupStatus status, long page, long size) {
        LambdaQueryWrapper<Meetup> query = new LambdaQueryWrapper<Meetup>()
                .orderByDesc(Meetup::getCreatedAt)
                .orderByDesc(Meetup::getId);
        if (status != null) {
            query.eq(Meetup::getStatus, status);
        }
        return meetupMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    @Transactional(readOnly = true)
    public Meetup getVisible(Long viewerId, long meetupId) {
        Meetup meetup = requireMeetup(meetupId);
        if (!PUBLIC_STATUSES.contains(meetup.getStatus()) && !isInvolved(viewerId, meetup)) {
            throw new ResourceNotFoundException("活动不存在");
        }
        if (PUBLIC_STATUSES.contains(meetup.getStatus()) && !associationsArePublic(meetup)) {
            throw new ResourceNotFoundException("活动不存在");
        }
        return meetup;
    }

    @Override
    @Transactional
    public Meetup create(long creatorId, Long topicId, Long circleId, MeetupCommand command) {
        IdentityVerification creatorIdentity = ensureEligible(creatorId, "创建活动");
        Association association = resolveAssociation(topicId, circleId);
        validateCommand(command, creatorIdentity);
        LocalDateTime now = LocalDateTime.now(clock);
        Meetup meetup = Meetup.builder()
                .creatorAccountId(creatorId)
                .topicId(association.topicId())
                .circleId(association.circleId())
                .meetupMode(command.meetupMode())
                .title(command.title().trim())
                .description(command.description().trim())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .applicationDeadline(command.applicationDeadline())
                .city(trim(command.city()))
                .district(trim(command.district()))
                .locationName(trim(command.locationName()))
                .address(trim(command.address()))
                .capacity(command.capacity())
                .minimumAge(command.minimumAge())
                .maximumAge(command.maximumAge())
                .genderRequirement(command.genderRequirement())
                .skillRequirement(trim(command.skillRequirement()))
                .status(MeetupStatus.DRAFT)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        meetupMapper.insert(meetup);
        syncOnlineDetail(meetup.getId(), command, now);
        participantMapper.insert(MeetupParticipant.builder()
                .meetupId(meetup.getId())
                .accountId(creatorId)
                .role(ParticipantRole.CREATOR)
                .status(ParticipantStatus.ACCEPTED)
                .decidedAt(now)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
        audit(meetup.getId(), creatorId, creatorId, MeetupAuditAction.CREATE, null, now);
        return meetup;
    }

    @Override
    @Transactional
    public Meetup update(long creatorId, long meetupId, MeetupCommand command) {
        Meetup meetup = requireCreator(creatorId, meetupId);
        if (meetup.getStatus() != MeetupStatus.DRAFT) {
            throw new ConflictException("只有草稿活动可以编辑");
        }
        validateCommand(command, ensureEligible(creatorId, "编辑活动"));
        applyCommand(meetup, command);
        LocalDateTime now = LocalDateTime.now(clock);
        meetup.setUpdatedAt(now);
        persist(meetup);
        syncOnlineDetail(meetupId, command, now);
        audit(meetupId, creatorId, null, MeetupAuditAction.UPDATE, null, now);
        return meetup;
    }

    @Override
    @Transactional
    public Meetup publish(long creatorId, long meetupId) {
        Meetup meetup = requireCreator(creatorId, meetupId);
        if (meetup.getStatus() != MeetupStatus.DRAFT) {
            throw new ConflictException("只有草稿活动可以发布");
        }
        ensureEligible(creatorId, "发布活动");
        validateAssociation(meetup);
        ensureNoTimeConflict(creatorId, meetup);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!meetup.getApplicationDeadline().isAfter(now)) {
            throw new ConflictException("报名截止时间必须晚于当前时间");
        }
        meetup.setStatus(MeetupStatus.OPEN);
        meetup.setUpdatedAt(now);
        persist(meetup);
        audit(meetupId, creatorId, null, MeetupAuditAction.PUBLISH, null, now);
        return meetup;
    }

    @Override
    @Transactional
    public Meetup confirm(long creatorId, long meetupId) {
        Meetup meetup = meetupMapper.selectByIdForUpdate(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        requireCreator(creatorId, meetup);
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            throw new ConflictException("只有报名中的活动可以确认");
        }
        if (!meetup.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("活动开始后不能确认");
        }
        if (acceptedCount(meetupId) < 2) {
            throw new ConflictException("至少接受一名参与者后才能确认活动");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        meetup.setStatus(MeetupStatus.CONFIRMED);
        meetup.setUpdatedAt(now);
        persist(meetup);
        audit(meetupId, creatorId, null, MeetupAuditAction.CONFIRM, null, now);
        return meetup;
    }

    @Override
    @Transactional
    public Meetup cancel(long creatorId, long meetupId, String reason) {
        Meetup meetup = requireCreator(creatorId, meetupId);
        if (!List.of(MeetupStatus.DRAFT, MeetupStatus.OPEN, MeetupStatus.CONFIRMED).contains(meetup.getStatus())) {
            throw new ConflictException("当前活动状态不允许取消");
        }
        if (!meetup.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("活动开始后不能取消");
        }
        return close(meetup, creatorId, MeetupStatus.CANCELLED, MeetupAuditAction.CANCEL, reason);
    }

    @Override
    @Transactional
    public Meetup terminate(long operatorId, long meetupId, String reason) {
        Meetup meetup = requireMeetup(meetupId);
        if (!List.of(MeetupStatus.OPEN, MeetupStatus.CONFIRMED).contains(meetup.getStatus())) {
            throw new ConflictException("当前活动状态不允许终止");
        }
        return close(meetup, operatorId, MeetupStatus.TERMINATED, MeetupAuditAction.TERMINATE, reason);
    }

    @Override
    @Transactional
    public MeetupParticipant apply(long accountId, long meetupId, String message) {
        Meetup meetup = requireMeetup(meetupId);
        IdentityVerification identity = ensureEligible(accountId, "报名活动");
        ensureOpenForApplication(meetup);
        if (meetup.getCreatorAccountId().equals(accountId)) {
            throw new ConflictException("创建者不能申请自己的活动");
        }
        validateApplicant(identity, meetup);
        ensureNoTimeConflict(accountId, meetup);
        if (acceptedCount(meetupId) >= meetup.getCapacity()) {
            throw new ConflictException("活动人数已满");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        MeetupParticipant participant = findParticipant(meetupId, accountId);
        if (participant == null) {
            participant = MeetupParticipant.builder()
                    .meetupId(meetupId)
                    .accountId(accountId)
                    .role(ParticipantRole.MEMBER)
                    .status(ParticipantStatus.APPLIED)
                    .applicationMessage(trim(message))
                    .version(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            try {
                participantMapper.insert(participant);
            } catch (DuplicateKeyException exception) {
                throw new ConflictException("请勿重复申请活动", exception);
            }
        } else {
            if (!List.of(ParticipantStatus.REJECTED, ParticipantStatus.CANCELLED).contains(participant.getStatus())) {
                throw new ConflictException("请勿重复申请活动");
            }
            participant.setStatus(ParticipantStatus.APPLIED);
            participant.setApplicationMessage(trim(message));
            participant.setDecisionReason(null);
            participant.setDecidedAt(null);
            participant.setCancelledAt(null);
            participant.setUpdatedAt(now);
            persist(participant);
        }
        audit(meetupId, accountId, accountId, MeetupAuditAction.APPLY, null, now);
        return participant;
    }

    @Override
    @Transactional
    public MeetupParticipant decide(long creatorId, long meetupId, long applicantId, boolean accepted,
            String reason) {
        Meetup meetup = meetupMapper.selectByIdForUpdate(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        requireCreator(creatorId, meetup);
        ensureOpenForApplication(meetup);
        MeetupParticipant participant = requireParticipant(meetupId, applicantId);
        if (participant.getStatus() != ParticipantStatus.APPLIED) {
            throw new ConflictException("该申请不处于待处理状态");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (accepted) {
            if (acceptedCount(meetupId) >= meetup.getCapacity()) {
                throw new ConflictException("活动人数已满");
            }
            IdentityVerification identity = ensureEligible(applicantId, "参加活动");
            validateApplicant(identity, meetup);
            ensureNoTimeConflict(applicantId, meetup);
            participant.setStatus(ParticipantStatus.ACCEPTED);
            participant.setDecisionReason(null);
        } else {
            participant.setStatus(ParticipantStatus.REJECTED);
            participant.setDecisionReason(requireReason(reason));
        }
        participant.setDecidedAt(now);
        participant.setUpdatedAt(now);
        persist(participant);
        audit(meetupId, creatorId, applicantId,
                accepted ? MeetupAuditAction.ACCEPT : MeetupAuditAction.REJECT, trim(reason), now);
        return participant;
    }

    @Override
    @Transactional
    public MeetupParticipant withdraw(long accountId, long meetupId, String reason) {
        Meetup meetup = meetupMapper.selectByIdForUpdate(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        if (meetup.getCreatorAccountId().equals(accountId)) {
            throw new ConflictException("创建者应取消活动，不能退出自己的活动");
        }
        if (!meetup.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("活动开始后不能退出");
        }
        MeetupParticipant participant = requireParticipant(meetupId, accountId);
        if (!List.of(ParticipantStatus.APPLIED, ParticipantStatus.ACCEPTED).contains(participant.getStatus())) {
            throw new ConflictException("当前参与状态不允许退出");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        participant.setStatus(ParticipantStatus.CANCELLED);
        participant.setDecisionReason(trim(reason));
        participant.setCancelledAt(now);
        participant.setUpdatedAt(now);
        persist(participant);
        audit(meetupId, accountId, accountId, MeetupAuditAction.WITHDRAW, trim(reason), now);
        return participant;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<MeetupParticipant> listApplications(long creatorId, long meetupId, long page, long size) {
        requireCreator(creatorId, meetupId);
        return participantMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MeetupParticipant>()
                .eq(MeetupParticipant::getMeetupId, meetupId)
                .eq(MeetupParticipant::getRole, ParticipantRole.MEMBER)
                .orderByAsc(MeetupParticipant::getCreatedAt));
    }

    @Override
    @Transactional(readOnly = true)
    public long acceptedCount(long meetupId) {
        return participantMapper.selectCount(new LambdaQueryWrapper<MeetupParticipant>()
                .eq(MeetupParticipant::getMeetupId, meetupId)
                .eq(MeetupParticipant::getStatus, ParticipantStatus.ACCEPTED));
    }

    @Override
    @Transactional(readOnly = true)
    public MeetupOnlineDetail findOnlineDetail(long meetupId) {
        return onlineDetailMapper.selectById(meetupId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canSeePrivateDetails(Long viewerId, Meetup meetup) {
        return isInvolved(viewerId, meetup);
    }

    private LambdaQueryWrapper<Meetup> publicQuery() {
        return new LambdaQueryWrapper<Meetup>()
                .in(Meetup::getStatus, PUBLIC_STATUSES)
                .gt(Meetup::getStartTime, LocalDateTime.now(clock))
                .apply("EXISTS (SELECT 1 FROM topic t JOIN category c ON c.id = t.category_id "
                        + "WHERE t.id = meetup.topic_id AND t.enabled = TRUE AND c.enabled = TRUE)")
                .and(value -> value.isNull(Meetup::getCircleId)
                        .or().apply("EXISTS (SELECT 1 FROM circle ci WHERE ci.id = meetup.circle_id "
                                + "AND ci.status = 'APPROVED')"))
                .orderByAsc(Meetup::getStartTime)
                .orderByAsc(Meetup::getId);
    }

    private Association resolveAssociation(Long topicId, Long circleId) {
        if (circleId != null) {
            Circle circle = circleService.getPublic(circleId);
            if (topicId != null && !topicId.equals(circle.getTopicId())) {
                throw new ConflictException("Circle 与 Topic 不匹配");
            }
            return new Association(circle.getTopicId(), circle.getId());
        }
        if (topicId == null) {
            throw new ConflictException("活动必须关联 Topic 或 Circle");
        }
        Topic topic = catalogService.getTopic(topicId, false);
        return new Association(topic.getId(), null);
    }

    private void validateAssociation(Meetup meetup) {
        resolveAssociation(meetup.getTopicId(), meetup.getCircleId());
    }

    private boolean associationsArePublic(Meetup meetup) {
        try {
            validateAssociation(meetup);
            return true;
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }

    private void validateCommand(MeetupCommand command, IdentityVerification creatorIdentity) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!command.startTime().isAfter(now)) {
            throw new ConflictException("活动开始时间必须晚于当前时间");
        }
        if (!command.endTime().isAfter(command.startTime())) {
            throw new ConflictException("活动结束时间必须晚于开始时间");
        }
        if (!command.applicationDeadline().isAfter(now)
                || !command.applicationDeadline().isBefore(command.startTime())) {
            throw new ConflictException("报名截止时间必须在当前时间和活动开始时间之间");
        }
        if (command.maximumAge() < command.minimumAge()) {
            throw new ConflictException("最高年龄不能小于最低年龄");
        }
        validateLocation(command);
        int creatorAge = age(creatorIdentity);
        if (creatorAge < command.minimumAge() || creatorAge > command.maximumAge()) {
            throw new ConflictException("创建者本人必须符合活动年龄要求");
        }
    }

    private void applyCommand(Meetup meetup, MeetupCommand command) {
        meetup.setMeetupMode(command.meetupMode());
        meetup.setTitle(command.title().trim());
        meetup.setDescription(command.description().trim());
        meetup.setStartTime(command.startTime());
        meetup.setEndTime(command.endTime());
        meetup.setApplicationDeadline(command.applicationDeadline());
        meetup.setCity(trim(command.city()));
        meetup.setDistrict(trim(command.district()));
        meetup.setLocationName(trim(command.locationName()));
        meetup.setAddress(trim(command.address()));
        meetup.setCapacity(command.capacity());
        meetup.setMinimumAge(command.minimumAge());
        meetup.setMaximumAge(command.maximumAge());
        meetup.setGenderRequirement(command.genderRequirement());
        meetup.setSkillRequirement(trim(command.skillRequirement()));
    }

    private void validateLocation(MeetupCommand command) {
        if (command.meetupMode() == null) {
            throw new ConflictException("活动模式不能为空");
        }
        if (command.meetupMode() == MeetupMode.OFFLINE) {
            if (isBlank(command.city()) || isBlank(command.district())
                    || isBlank(command.locationName()) || isBlank(command.address())) {
                throw new ConflictException("线下活动必须填写城市、区域、地点名称和详细地址");
            }
            if (!isBlank(command.onlinePlatform()) || !isBlank(command.serverRegion())
                    || !isBlank(command.accessInstructions())) {
                throw new ConflictException("线下活动不能填写线上活动信息");
            }
            return;
        }
        if (!isBlank(command.city()) || !isBlank(command.district())
                || !isBlank(command.locationName()) || !isBlank(command.address())) {
            throw new ConflictException("线上活动不能填写线下地点信息");
        }
        if (isBlank(command.onlinePlatform()) || isBlank(command.accessInstructions())) {
            throw new ConflictException("线上活动必须填写线上平台和加入说明");
        }
    }

    private void syncOnlineDetail(long meetupId, MeetupCommand command, LocalDateTime now) {
        if (command.meetupMode() == MeetupMode.OFFLINE) {
            onlineDetailMapper.deleteById(meetupId);
            return;
        }
        MeetupOnlineDetail detail = onlineDetailMapper.selectById(meetupId);
        if (detail == null) {
            onlineDetailMapper.insert(MeetupOnlineDetail.builder()
                    .meetupId(meetupId)
                    .onlinePlatform(command.onlinePlatform().trim())
                    .serverRegion(trim(command.serverRegion()))
                    .accessInstructions(command.accessInstructions().trim())
                    .version(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            return;
        }
        detail.setOnlinePlatform(command.onlinePlatform().trim());
        detail.setServerRegion(trim(command.serverRegion()));
        detail.setAccessInstructions(command.accessInstructions().trim());
        detail.setUpdatedAt(now);
        if (onlineDetailMapper.updateById(detail) != 1) {
            throw new ConflictException("线上活动信息已发生变化，请刷新后重试");
        }
    }

    private IdentityVerification ensureEligible(long accountId, String action) {
        if (accountService.getById(accountId).getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("账户状态不允许" + action);
        }
        IdentityVerification identity = identityService.findByAccountId(accountId);
        if (identity == null || identity.getStatus() != IdentityStatus.VERIFIED) {
            throw new ForbiddenException("完成实名认证后才能" + action);
        }
        if (!agePolicy.isAdult(identity.getBirthDate())) {
            throw new ForbiddenException("未满 18 周岁不能" + action);
        }
        return identity;
    }

    private void validateApplicant(IdentityVerification identity, Meetup meetup) {
        int applicantAge = age(identity);
        if (applicantAge < meetup.getMinimumAge() || applicantAge > meetup.getMaximumAge()) {
            throw new ForbiddenException("年龄不符合活动要求");
        }
        if (meetup.getGenderRequirement() == GenderRequirement.SAME_GENDER) {
            IdentityVerification creator = identityService.findByAccountId(meetup.getCreatorAccountId());
            if (creator == null || creator.getGender() != identity.getGender()) {
                throw new ForbiddenException("性别不符合活动要求");
            }
        }
    }

    private int age(IdentityVerification identity) {
        return Period.between(identity.getBirthDate(), LocalDate.now(clock)).getYears();
    }

    private void ensureOpenForApplication(Meetup meetup) {
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            throw new ConflictException("活动当前不接受报名");
        }
        if (!meetup.getApplicationDeadline().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("活动报名已经截止");
        }
    }

    private void ensureNoTimeConflict(long accountId, Meetup meetup) {
        if (meetupMapper.hasAcceptedTimeConflict(accountId, meetup.getStartTime(), meetup.getEndTime(),
                meetup.getId())) {
            throw new ConflictException("活动时间与已参加活动冲突");
        }
    }

    private Meetup close(Meetup meetup, long operatorId, MeetupStatus status, MeetupAuditAction action,
            String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        meetup.setStatus(status);
        meetup.setClosedReason(requireReason(reason));
        meetup.setClosedBy(operatorId);
        meetup.setClosedAt(now);
        meetup.setUpdatedAt(now);
        persist(meetup);
        audit(meetup.getId(), operatorId, null, action, reason.trim(), now);
        return meetup;
    }

    private Meetup requireMeetup(long meetupId) {
        Meetup meetup = meetupMapper.selectById(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        return meetup;
    }

    private Meetup requireCreator(long creatorId, long meetupId) {
        return requireCreator(creatorId, requireMeetup(meetupId));
    }

    private Meetup requireCreator(long creatorId, Meetup meetup) {
        if (!meetup.getCreatorAccountId().equals(creatorId)) {
            throw new ForbiddenException("只有活动创建者可以执行此操作");
        }
        return meetup;
    }

    private MeetupParticipant findParticipant(long meetupId, long accountId) {
        return participantMapper.selectOne(new LambdaQueryWrapper<MeetupParticipant>()
                .eq(MeetupParticipant::getMeetupId, meetupId)
                .eq(MeetupParticipant::getAccountId, accountId));
    }

    private MeetupParticipant requireParticipant(long meetupId, long accountId) {
        MeetupParticipant participant = findParticipant(meetupId, accountId);
        if (participant == null) {
            throw new ResourceNotFoundException("活动申请不存在");
        }
        return participant;
    }

    private boolean isInvolved(Long viewerId, Meetup meetup) {
        return viewerId != null && (meetup.getCreatorAccountId().equals(viewerId)
                || participantMapper.selectCount(new LambdaQueryWrapper<MeetupParticipant>()
                        .eq(MeetupParticipant::getMeetupId, meetup.getId())
                        .eq(MeetupParticipant::getAccountId, viewerId)
                        .eq(MeetupParticipant::getStatus, ParticipantStatus.ACCEPTED)) > 0);
    }

    private void persist(Meetup meetup) {
        if (meetupMapper.updateById(meetup) != 1) {
            throw new ConflictException("活动已发生变化，请刷新后重试");
        }
    }

    private void persist(MeetupParticipant participant) {
        if (participantMapper.updateById(participant) != 1) {
            throw new ConflictException("参与状态已发生变化，请刷新后重试");
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ConflictException("必须填写原因");
        }
        return reason.trim();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void audit(long meetupId, long operatorId, Long participantId, MeetupAuditAction action,
            String reason, LocalDateTime now) {
        auditMapper.insert(MeetupAuditLog.builder()
                .meetupId(meetupId)
                .operatorAccountId(operatorId)
                .participantAccountId(participantId)
                .action(action)
                .reason(reason)
                .occurredAt(now)
                .build());
    }

    private record Association(Long topicId, Long circleId) {
    }
}
