package com.austin.module.meetup.service;

import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.MeetupOnlineDetail;
import com.austin.module.meetup.domain.MeetupStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface MeetupService {

    IPage<Meetup> listPublic(Long topicId, Long circleId, long page, long size);

    IPage<Meetup> listMine(long accountId, long page, long size);

    IPage<Meetup> listForAdmin(MeetupStatus status, long page, long size);

    Meetup getVisible(Long viewerId, long meetupId);

    Meetup create(long creatorId, Long topicId, Long circleId, MeetupCommand command);

    Meetup update(long creatorId, long meetupId, MeetupCommand command);

    Meetup publish(long creatorId, long meetupId);

    Meetup confirm(long creatorId, long meetupId);

    Meetup complete(long creatorId, long meetupId);

    Meetup cancel(long creatorId, long meetupId, String reason);

    Meetup terminate(long operatorId, long meetupId, String reason);

    MeetupParticipant apply(long accountId, long meetupId, String message);

    MeetupParticipant decide(long creatorId, long meetupId, long applicantId, boolean accepted, String reason);

    MeetupParticipant withdraw(long accountId, long meetupId, String reason);

    IPage<MeetupParticipant> listApplications(long creatorId, long meetupId, long page, long size);

    long acceptedCount(long meetupId);

    MeetupOnlineDetail findOnlineDetail(long meetupId);

    boolean canSeePrivateDetails(Long viewerId, Meetup meetup);
}
