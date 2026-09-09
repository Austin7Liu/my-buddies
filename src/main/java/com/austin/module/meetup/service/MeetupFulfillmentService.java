package com.austin.module.meetup.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.meetup.domain.FulfillmentResult;
import com.austin.module.meetup.domain.FulfillmentSource;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupAuditAction;
import com.austin.module.meetup.domain.MeetupAuditLog;
import com.austin.module.meetup.domain.MeetupCheckIn;
import com.austin.module.meetup.domain.MeetupFulfillment;
import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.MeetupStatus;
import com.austin.module.meetup.domain.ParticipantStatus;
import com.austin.module.meetup.mapper.MeetupAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupCheckInMapper;
import com.austin.module.meetup.mapper.MeetupFulfillmentMapper;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.meetup.mapper.MeetupParticipantMapper;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetupFulfillmentService {

    private final MeetupMapper meetupMapper;
    private final MeetupParticipantMapper participantMapper;
    private final MeetupCheckInMapper checkInMapper;
    private final MeetupFulfillmentMapper fulfillmentMapper;
    private final MeetupAuditLogMapper auditMapper;
    private final Clock clock;
    private final NotificationService notificationService;

    @Transactional
    public void settle(Meetup meetup, LocalDateTime settledAt) {
        if (meetup.getMeetupMode() == MeetupMode.ONLINE) {
            return;
        }
        Set<Long> checkedInAccountIds = checkInMapper.selectList(new LambdaQueryWrapper<MeetupCheckIn>()
                        .eq(MeetupCheckIn::getMeetupId, meetup.getId()))
                .stream()
                .map(MeetupCheckIn::getAccountId)
                .collect(Collectors.toSet());
        for (MeetupParticipant participant : acceptedParticipants(meetup.getId())) {
            boolean checkedIn = checkedInAccountIds.contains(participant.getAccountId());
            fulfillmentMapper.insert(MeetupFulfillment.builder()
                    .meetupId(meetup.getId())
                    .accountId(participant.getAccountId())
                    .result(checkedIn ? FulfillmentResult.ATTENDED : FulfillmentResult.ABSENT)
                    .source(checkedIn ? FulfillmentSource.CHECK_IN : FulfillmentSource.SYSTEM)
                    .settledAt(settledAt)
                    .version(0)
                    .createdAt(settledAt)
                    .updatedAt(settledAt)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public MeetupFulfillment getMine(long accountId, long meetupId) {
        MeetupFulfillment fulfillment = find(meetupId, accountId);
        if (fulfillment == null) {
            throw new ResourceNotFoundException("履约结果不存在");
        }
        return fulfillment;
    }

    @Transactional(readOnly = true)
    public IPage<MeetupFulfillment> list(long creatorId, long meetupId, long page, long size) {
        Meetup meetup = requireMeetup(meetupId);
        if (!meetup.getCreatorAccountId().equals(creatorId)) {
            throw new ForbiddenException("只有活动创建者可以查看履约结果");
        }
        return listPage(meetupId, page, size);
    }

    @Transactional(readOnly = true)
    public IPage<MeetupFulfillment> listForAdmin(long meetupId, long page, long size) {
        requireMeetup(meetupId);
        return listPage(meetupId, page, size);
    }

    @Transactional
    public MeetupFulfillment adjust(long operatorId, long meetupId, long accountId,
            FulfillmentResult result, String reason) {
        if (result != FulfillmentResult.ATTENDED && result != FulfillmentResult.EXCUSED) {
            throw new ConflictException("管理员只能将履约结果修正为出席或请假");
        }
        if (reason == null || reason.isBlank()) {
            throw new ConflictException("修正履约结果必须填写原因");
        }
        String normalizedReason = reason.trim();
        Meetup meetup = requireMeetup(meetupId);
        if (meetup.getMeetupMode() != MeetupMode.OFFLINE || meetup.getStatus() != MeetupStatus.COMPLETED) {
            throw new ConflictException("只有已完成的线下活动可以修正履约结果");
        }
        MeetupFulfillment fulfillment = getMine(accountId, meetupId);
        if (fulfillment.getResult() == result) {
            throw new ConflictException("履约结果无需修改");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        fulfillment.setResult(result);
        fulfillment.setSource(FulfillmentSource.ADMIN_OVERRIDE);
        fulfillment.setAdjustedBy(operatorId);
        fulfillment.setAdjustmentReason(normalizedReason);
        fulfillment.setUpdatedAt(now);
        if (fulfillmentMapper.updateById(fulfillment) != 1) {
            throw new ConflictException("履约结果已发生变化，请刷新后重试");
        }
        auditMapper.insert(MeetupAuditLog.builder()
                .meetupId(meetupId)
                .operatorAccountId(operatorId)
                .participantAccountId(accountId)
                .action(MeetupAuditAction.ADJUST_RESULT)
                .reason(normalizedReason)
                .occurredAt(now)
                .build());
        notificationService.notify(accountId, NotificationType.FULFILLMENT_ADJUSTED,
                "活动履约结果已修正", "修正结果：" + result.name(),
                NotificationReferenceType.MEETUP, meetupId,
                "meetup:" + meetupId + ":fulfillment:" + accountId + ":" + fulfillment.getVersion());
        return fulfillment;
    }

    private List<MeetupParticipant> acceptedParticipants(long meetupId) {
        return participantMapper.selectList(new LambdaQueryWrapper<MeetupParticipant>()
                .eq(MeetupParticipant::getMeetupId, meetupId)
                .eq(MeetupParticipant::getStatus, ParticipantStatus.ACCEPTED));
    }

    private MeetupFulfillment find(long meetupId, long accountId) {
        return fulfillmentMapper.selectOne(new LambdaQueryWrapper<MeetupFulfillment>()
                .eq(MeetupFulfillment::getMeetupId, meetupId)
                .eq(MeetupFulfillment::getAccountId, accountId));
    }

    private IPage<MeetupFulfillment> listPage(long meetupId, long page, long size) {
        return fulfillmentMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MeetupFulfillment>()
                .eq(MeetupFulfillment::getMeetupId, meetupId)
                .orderByAsc(MeetupFulfillment::getAccountId));
    }

    private Meetup requireMeetup(long meetupId) {
        Meetup meetup = meetupMapper.selectById(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        return meetup;
    }
}
