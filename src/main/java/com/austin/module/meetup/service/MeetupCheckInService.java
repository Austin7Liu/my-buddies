package com.austin.module.meetup.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupAuditAction;
import com.austin.module.meetup.domain.MeetupAuditLog;
import com.austin.module.meetup.domain.MeetupCheckIn;
import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.MeetupStatus;
import com.austin.module.meetup.domain.ParticipantStatus;
import com.austin.module.meetup.mapper.MeetupAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupCheckInMapper;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.meetup.mapper.MeetupParticipantMapper;
import com.austin.module.meetup.support.GeoDistanceCalculator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetupCheckInService {

    private final MeetupMapper meetupMapper;
    private final MeetupParticipantMapper participantMapper;
    private final MeetupCheckInMapper checkInMapper;
    private final MeetupAuditLogMapper auditMapper;
    private final GeoDistanceCalculator distanceCalculator;
    private final Clock clock;

    @Transactional
    public MeetupCheckIn checkIn(long accountId, long meetupId, BigDecimal latitude, BigDecimal longitude) {
        Meetup meetup = requireMeetup(meetupId);
        ensureAccepted(meetupId, accountId);
        MeetupCheckIn existing = find(meetupId, accountId);
        if (existing != null) {
            return existing;
        }
        if (meetup.getMeetupMode() != MeetupMode.OFFLINE) {
            throw new ConflictException("线上活动不支持签到");
        }
        if (meetup.getStatus() != MeetupStatus.CONFIRMED) {
            throw new ConflictException("只有已确认活动可以签到");
        }
        if (meetup.getLocationLatitude() == null || meetup.getLocationLongitude() == null
                || meetup.getCheckInRadiusMeters() == null) {
            throw new ConflictException("该活动未配置签到位置");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(meetup.getStartTime().minusMinutes(30))) {
            throw new ConflictException("尚未到签到时间");
        }
        if (now.isAfter(meetup.getEndTime())) {
            throw new ConflictException("签到时间已经结束");
        }
        int distance = (int) Math.round(distanceCalculator.distanceMeters(
                meetup.getLocationLatitude().doubleValue(), meetup.getLocationLongitude().doubleValue(),
                latitude.doubleValue(), longitude.doubleValue()));
        if (distance > meetup.getCheckInRadiusMeters()) {
            throw new ConflictException("当前位置超出签到范围");
        }
        MeetupCheckIn checkIn = MeetupCheckIn.builder()
                .meetupId(meetupId)
                .accountId(accountId)
                .distanceMeters(distance)
                .checkedInAt(now)
                .createdAt(now)
                .build();
        try {
            checkInMapper.insert(checkIn);
        } catch (DuplicateKeyException exception) {
            return find(meetupId, accountId);
        }
        auditMapper.insert(MeetupAuditLog.builder()
                .meetupId(meetupId)
                .operatorAccountId(accountId)
                .participantAccountId(accountId)
                .action(MeetupAuditAction.CHECK_IN)
                .occurredAt(now)
                .build());
        return checkIn;
    }

    @Transactional(readOnly = true)
    public MeetupCheckIn getMine(long accountId, long meetupId) {
        ensureAccepted(meetupId, accountId);
        MeetupCheckIn checkIn = find(meetupId, accountId);
        if (checkIn == null) {
            throw new ResourceNotFoundException("签到记录不存在");
        }
        return checkIn;
    }

    @Transactional(readOnly = true)
    public IPage<MeetupCheckIn> list(long creatorId, long meetupId, long page, long size) {
        Meetup meetup = requireMeetup(meetupId);
        if (!meetup.getCreatorAccountId().equals(creatorId)) {
            throw new ForbiddenException("只有活动创建者可以查看签到名单");
        }
        return checkInMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MeetupCheckIn>()
                .eq(MeetupCheckIn::getMeetupId, meetupId)
                .orderByAsc(MeetupCheckIn::getCheckedInAt));
    }

    private Meetup requireMeetup(long meetupId) {
        Meetup meetup = meetupMapper.selectById(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        return meetup;
    }

    private void ensureAccepted(long meetupId, long accountId) {
        if (participantMapper.selectCount(new LambdaQueryWrapper<MeetupParticipant>()
                .eq(MeetupParticipant::getMeetupId, meetupId)
                .eq(MeetupParticipant::getAccountId, accountId)
                .eq(MeetupParticipant::getStatus, ParticipantStatus.ACCEPTED)) != 1) {
            throw new ForbiddenException("只有已接受参与者可以签到");
        }
    }

    private MeetupCheckIn find(long meetupId, long accountId) {
        return checkInMapper.selectOne(new LambdaQueryWrapper<MeetupCheckIn>()
                .eq(MeetupCheckIn::getMeetupId, meetupId)
                .eq(MeetupCheckIn::getAccountId, accountId));
    }
}
