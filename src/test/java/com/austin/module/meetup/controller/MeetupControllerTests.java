package com.austin.module.meetup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.service.CircleService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupAuditAction;
import com.austin.module.meetup.domain.MeetupAuditLog;
import com.austin.module.meetup.domain.FulfillmentResult;
import com.austin.module.meetup.domain.FulfillmentSource;
import com.austin.module.meetup.domain.MeetupFulfillment;
import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.domain.MeetupReviewAuditAction;
import com.austin.module.meetup.domain.MeetupReviewAuditLog;
import com.austin.module.meetup.domain.ParticipantRole;
import com.austin.module.meetup.domain.ParticipantStatus;
import com.austin.module.meetup.mapper.MeetupAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupFulfillmentMapper;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.meetup.mapper.MeetupParticipantMapper;
import com.austin.module.meetup.mapper.MeetupReviewAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupReviewMapper;
import com.austin.module.risk.domain.AccountBusinessRestriction;
import com.austin.module.risk.domain.AccountBusinessRestrictionAuditLog;
import com.austin.module.risk.domain.RestrictionAuditAction;
import com.austin.module.risk.domain.RestrictionStatus;
import com.austin.module.risk.domain.RestrictionType;
import com.austin.module.risk.mapper.AccountBusinessRestrictionAuditLogMapper;
import com.austin.module.risk.mapper.AccountBusinessRestrictionMapper;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.domain.UserNotification;
import com.austin.module.notification.mapper.UserNotificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MeetupControllerTests {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService accountService;

    @Autowired
    private IdentityVerificationService identityService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CircleService circleService;

    @Autowired
    private MeetupMapper meetupMapper;

    @Autowired
    private MeetupParticipantMapper participantMapper;

    @Autowired
    private MeetupAuditLogMapper auditMapper;

    @Autowired
    private MeetupFulfillmentMapper fulfillmentMapper;

    @Autowired
    private MeetupReviewMapper reviewMapper;

    @Autowired
    private MeetupReviewAuditLogMapper reviewAuditMapper;

    @Autowired
    private AccountBusinessRestrictionMapper restrictionMapper;

    @Autowired
    private AccountBusinessRestrictionAuditLogMapper restrictionAuditMapper;

    @Autowired
    private UserNotificationMapper notificationMapper;

    private UserAccount creator;
    private UserAccount applicant;
    private UserAccount moderator;
    private Topic topic;
    private Circle circle;

    @BeforeEach
    void setUp() {
        creator = verifiedAccount("13900139600", "创建者", "110105198806150016");
        applicant = verifiedAccount("13900139601", "申请者", "11010519491231002X");
        moderator = accountService.create("13900139602");
        topic = catalogService.createTopic(moderator.getId(), 101, "meetup-test-topic", "活动测试话题", null, 1);
        circle = circleService.create(creator.getId(), topic.getId(), "活动测试圈子", null, "杭州", "滨江");
        circleService.approve(moderator.getId(), circle.getId());
    }

    @Test
    void circleMeetupGetsTopicAndCreatorOccupiesOneSlot() throws Exception {
        createMeetup(null, circle.getId(), 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.data.circleId").value(circle.getId()))
                .andExpect(jsonPath("$.data.creator.accountId").value(creator.getId()))
                .andExpect(jsonPath("$.data.creator.avatarCode").value("PANDA"))
                .andExpect(jsonPath("$.data.creator.verified").value(true))
                .andExpect(jsonPath("$.data.meetupMode").value("OFFLINE"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.acceptedCount").value(1))
                .andExpect(jsonPath("$.data.remainingSlots").value(3));

        Meetup meetup = findMeetup();
        var creatorParticipant = participantMapper.selectOne(new LambdaQueryWrapper<MeetupParticipant>()
                .eq(MeetupParticipant::getMeetupId, meetup.getId())
                .eq(MeetupParticipant::getAccountId, creator.getId()));
        assertThat(creatorParticipant.getRole()).isEqualTo(ParticipantRole.CREATOR);
        assertThat(creatorParticipant.getStatus()).isEqualTo(ParticipantStatus.ACCEPTED);
    }

    @Test
    void reputationWithoutHistoryReturnsZeroCountsAndNoRates() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/{accountId}/reputation", moderator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(moderator.getId()))
                .andExpect(jsonPath("$.data.offlineFulfillmentCount").value(0))
                .andExpect(jsonPath("$.data.attendedCount").value(0))
                .andExpect(jsonPath("$.data.absentCount").value(0))
                .andExpect(jsonPath("$.data.excusedCount").value(0))
                .andExpect(jsonPath("$.data.attendanceRate").doesNotExist())
                .andExpect(jsonPath("$.data.receivedReviewCount").value(0))
                .andExpect(jsonPath("$.data.averageRating").doesNotExist());
    }

    @Test
    void riskRestrictionBlocksMeetupActionsAndCanBeRevokedOrExpired() throws Exception {
        String createRestriction = "{\"accountId\":" + creator.getId()
                + ",\"restrictionType\":\"MEETUP_CREATE_DISABLED\","
                + "\"reason\":\"连续无故缺席\",\"expiresAt\":\""
                + LocalDateTime.now().plusDays(2).format(FORMATTER) + "\"}";
        mockMvc.perform(post("/api/v1/admin/risk/restrictions")
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRestriction))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/risk/restrictions")
                        .with(user(moderator.getId().toString()).roles("RISK_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRestriction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.effective").value(true));
        mockMvc.perform(post("/api/v1/admin/risk/restrictions")
                        .with(user(moderator.getId().toString()).roles("RISK_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRestriction))
                .andExpect(status().isConflict());
        createMeetup(topic.getId(), null, 3)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RESTRICTED"));
        mockMvc.perform(get("/api/v1/risk/restrictions/me")
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        UserNotification restrictionNotification = notificationMapper.selectOne(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getRecipientAccountId, creator.getId())
                        .eq(UserNotification::getNotificationType,
                                NotificationType.RISK_RESTRICTION_CREATED));
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", restrictionNotification.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", restrictionNotification.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        AccountBusinessRestriction createLimit = restrictionMapper.selectOne(
                new LambdaQueryWrapper<AccountBusinessRestriction>()
                        .eq(AccountBusinessRestriction::getAccountId, creator.getId())
                        .eq(AccountBusinessRestriction::getRestrictionType,
                                RestrictionType.MEETUP_CREATE_DISABLED));
        mockMvc.perform(post("/api/v1/admin/risk/restrictions/{restrictionId}/revoke", createLimit.getId())
                        .with(user(moderator.getId().toString()).roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"复核后撤销\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.effective").value(false));

        createMeetup(topic.getId(), null, 3).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        publish(meetup.getId());
        String joinRestriction = "{\"accountId\":" + applicant.getId()
                + ",\"restrictionType\":\"MEETUP_JOIN_DISABLED\","
                + "\"reason\":\"报名风险复核\",\"expiresAt\":\""
                + LocalDateTime.now().plusDays(2).format(FORMATTER) + "\"}";
        mockMvc.perform(post("/api/v1/admin/risk/restrictions")
                        .with(user(moderator.getId().toString()).roles("RISK_REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinRestriction))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"申请加入\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RESTRICTED"));

        AccountBusinessRestriction joinLimit = restrictionMapper.selectOne(
                new LambdaQueryWrapper<AccountBusinessRestriction>()
                        .eq(AccountBusinessRestriction::getAccountId, applicant.getId())
                        .eq(AccountBusinessRestriction::getRestrictionType,
                                RestrictionType.MEETUP_JOIN_DISABLED));
        joinLimit.setStartsAt(LocalDateTime.now().minusDays(3));
        joinLimit.setExpiresAt(LocalDateTime.now().minusDays(1));
        assertThat(restrictionMapper.updateById(joinLimit)).isEqualTo(1);
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"限制已到期\"}"))
                .andExpect(status().isOk());
        AccountBusinessRestriction expired = restrictionMapper.selectById(joinLimit.getId());
        assertThat(expired.getStatus()).isEqualTo(RestrictionStatus.EXPIRED);
        assertThat(restrictionAuditMapper.selectCount(
                new LambdaQueryWrapper<AccountBusinessRestrictionAuditLog>()
                        .in(AccountBusinessRestrictionAuditLog::getAction,
                                RestrictionAuditAction.CREATE,
                                RestrictionAuditAction.REVOKE,
                                RestrictionAuditAction.EXPIRE)))
                .isEqualTo(4);
    }

    @Test
    void draftIsHiddenAndPublishedMeetupHidesAddressFromVisitor() throws Exception {
        createMeetup(topic.getId(), null, 4).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        mockMvc.perform(get("/api/v1/meetups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        publish(meetup.getId());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}", meetup.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").doesNotExist());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("滨江区网商路 1 号"));
    }

    @Test
    void applicantCanBeAcceptedAndCreatorCanConfirm() throws Exception {
        createMeetup(topic.getId(), null, 2).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        publish(meetup.getId());

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"想参加活动\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.profile.accountId").value(applicant.getId()))
                .andExpect(jsonPath("$.data.profile.avatarCode").value("PANDA"))
                .andExpect(jsonPath("$.data.profile.verified").value(true));
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications/{accountId}/accept",
                        meetup.getId(), applicant.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/confirm", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.acceptedCount").value(2))
                .andExpect(jsonPath("$.data.remainingSlots").value(0));
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    void confirmedMeetupCanOnlyBeCompletedByCreatorAfterEndTime() throws Exception {
        createMeetup(topic.getId(), null, 2).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        publish(meetup.getId());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"想参加活动\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications/{accountId}/accept",
                        meetup.getId(), applicant.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/confirm", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/complete", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("活动结束后才能完成"));

        Meetup confirmed = meetupMapper.selectById(meetup.getId());
        LocalDateTime now = LocalDateTime.now();
        confirmed.setApplicationDeadline(now.minusHours(3));
        confirmed.setStartTime(now.minusHours(2));
        confirmed.setEndTime(now.minusHours(1));
        assertThat(meetupMapper.updateById(confirmed)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/complete", meetup.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/complete", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/complete", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("只有已确认活动可以完成"));

        assertThat(fulfillmentMapper.selectList(new LambdaQueryWrapper<MeetupFulfillment>()
                .eq(MeetupFulfillment::getMeetupId, meetup.getId())))
                .hasSize(2)
                .allSatisfy(fulfillment -> {
                    assertThat(fulfillment.getResult()).isEqualTo(FulfillmentResult.ABSENT);
                    assertThat(fulfillment.getSource()).isEqualTo(FulfillmentSource.SYSTEM);
                });
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/reviews", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revieweeAccountId\":" + creator.getId()
                                + ",\"rating\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("只有实际出席的参与者之间可以互评"));
        mockMvc.perform(get("/api/v1/meetups/{meetupId}/fulfillment/me", meetup.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("ABSENT"));
        mockMvc.perform(get("/api/v1/meetups/{meetupId}/fulfillments", meetup.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}/fulfillments", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(patch("/api/v1/admin/meetups/{meetupId}/fulfillments/{accountId}",
                        meetup.getId(), applicant.getId())
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"EXCUSED\",\"reason\":\"定位异常，人工核实\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("EXCUSED"))
                .andExpect(jsonPath("$.data.source").value("ADMIN_OVERRIDE"))
                .andExpect(jsonPath("$.data.adjustedBy").value(moderator.getId()));
        mockMvc.perform(get("/api/v1/profiles/{accountId}/reputation", applicant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offlineFulfillmentCount").value(1))
                .andExpect(jsonPath("$.data.attendedCount").value(0))
                .andExpect(jsonPath("$.data.absentCount").value(0))
                .andExpect(jsonPath("$.data.excusedCount").value(1))
                .andExpect(jsonPath("$.data.attendanceRate").doesNotExist());

        assertThat(auditMapper.selectCount(new LambdaQueryWrapper<MeetupAuditLog>()
                .eq(MeetupAuditLog::getMeetupId, meetup.getId())
                .eq(MeetupAuditLog::getAction, MeetupAuditAction.COMPLETE)))
                .isEqualTo(1);
        assertThat(auditMapper.selectCount(new LambdaQueryWrapper<MeetupAuditLog>()
                .eq(MeetupAuditLog::getMeetupId, meetup.getId())
                .eq(MeetupAuditLog::getAction, MeetupAuditAction.ADJUST_RESULT)))
                .isEqualTo(1);
    }

    @Test
    void acceptedParticipantCanCheckInOnceWithinTimeAndDistance() throws Exception {
        createMeetup(topic.getId(), null, 2).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        publish(meetup.getId());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"参加\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications/{accountId}/accept",
                        meetup.getId(), applicant.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/confirm", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());

        Meetup confirmed = meetupMapper.selectById(meetup.getId());
        LocalDateTime now = LocalDateTime.now();
        confirmed.setApplicationDeadline(now.minusHours(2));
        confirmed.setStartTime(now.minusMinutes(5));
        confirmed.setEndTime(now.plusHours(1));
        assertThat(meetupMapper.updateById(confirmed)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/check-ins", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":31.2304,\"longitude\":121.4737}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("当前位置超出签到范围"));

        String nearbyLocation = "{\"latitude\":30.2084,\"longitude\":120.2123}";
        String firstResponse = mockMvc.perform(post("/api/v1/meetups/{meetupId}/check-ins", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nearbyLocation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(applicant.getId()))
                .andExpect(jsonPath("$.data.distanceMeters").value(0))
                .andReturn().getResponse().getContentAsString();
        String repeatedResponse = mockMvc.perform(post("/api/v1/meetups/{meetupId}/check-ins", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nearbyLocation))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(repeatedResponse).isEqualTo(firstResponse);

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/check-ins", meetup.getId())
                        .with(user(creator.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nearbyLocation))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/meetups/{meetupId}/check-ins", meetup.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}/check-ins", meetup.getId())
                .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        Meetup started = meetupMapper.selectById(meetup.getId());
        started.setEndTime(LocalDateTime.now().minusMinutes(1));
        assertThat(meetupMapper.updateById(started)).isEqualTo(1);
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/complete", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());
        MeetupFulfillment applicantFulfillment = fulfillmentMapper.selectOne(
                new LambdaQueryWrapper<MeetupFulfillment>()
                        .eq(MeetupFulfillment::getMeetupId, meetup.getId())
                        .eq(MeetupFulfillment::getAccountId, applicant.getId()));
        assertThat(applicantFulfillment.getResult()).isEqualTo(FulfillmentResult.ATTENDED);
        assertThat(applicantFulfillment.getSource()).isEqualTo(FulfillmentSource.CHECK_IN);

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/reviews", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revieweeAccountId\":" + creator.getId()
                                + ",\"rating\":5,\"comment\":\"准时友善\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.reviewer.accountId").value(applicant.getId()))
                .andExpect(jsonPath("$.data.reviewee.accountId").value(creator.getId()));
        MeetupReview review = reviewMapper.selectOne(new LambdaQueryWrapper<MeetupReview>()
                .eq(MeetupReview::getMeetupId, meetup.getId())
                .eq(MeetupReview::getReviewerAccountId, applicant.getId()));
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/reviews", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revieweeAccountId\":" + creator.getId()
                                + ",\"rating\":4}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/meetups/{meetupId}/reviews/{reviewId}", meetup.getId(), review.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"沟通顺畅\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4));
        mockMvc.perform(get("/api/v1/profiles/{accountId}/reviews", creator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/v1/profiles/{accountId}/reputation", creator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offlineFulfillmentCount").value(1))
                .andExpect(jsonPath("$.data.attendedCount").value(1))
                .andExpect(jsonPath("$.data.absentCount").value(0))
                .andExpect(jsonPath("$.data.excusedCount").value(0))
                .andExpect(jsonPath("$.data.attendanceRate").value(100.00))
                .andExpect(jsonPath("$.data.receivedReviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(4.00));
        mockMvc.perform(patch("/api/v1/admin/meetup-reviews/{reviewId}/hide", review.getId())
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"包含不当内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
        mockMvc.perform(get("/api/v1/profiles/{accountId}/reviews", creator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/profiles/{accountId}/reputation", creator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receivedReviewCount").value(0))
                .andExpect(jsonPath("$.data.averageRating").doesNotExist());
        mockMvc.perform(patch("/api/v1/admin/meetup-reviews/{reviewId}/restore", review.getId())
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"复核后恢复\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VISIBLE"));
        assertThat(reviewAuditMapper.selectCount(new LambdaQueryWrapper<MeetupReviewAuditLog>()
                .eq(MeetupReviewAuditLog::getReviewId, review.getId())
                .in(MeetupReviewAuditLog::getAction,
                        MeetupReviewAuditAction.HIDE, MeetupReviewAuditAction.RESTORE)))
                .isEqualTo(2);

        Meetup completed = meetupMapper.selectById(meetup.getId());
        completed.setCompletedAt(LocalDateTime.now().minusDays(8));
        assertThat(meetupMapper.updateById(completed)).isEqualTo(1);
        mockMvc.perform(put("/api/v1/meetups/{meetupId}/reviews/{reviewId}", meetup.getId(), review.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("活动评价期已结束"));
    }

    @Test
    void creatorCannotConfirmWithoutAnotherAcceptedParticipant() throws Exception {
        createMeetup(topic.getId(), null, 2).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        publish(meetup.getId());
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/confirm", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("至少接受一名参与者后才能确认活动"));
    }

    @Test
    void mismatchedTopicAndCircleAreRejected() throws Exception {
        Topic another = catalogService.createTopic(moderator.getId(), 101, "meetup-other-topic", "其他话题", null, 2);
        createMeetup(another.getId(), circle.getId(), 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Circle 与 Topic 不匹配"));
    }

    @Test
    void unverifiedUserCannotCreateMeetup() throws Exception {
        UserAccount unverified = accountService.create("13900139603");
        mockMvc.perform(post("/api/v1/meetups")
                        .with(user(unverified.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(topic.getId(), null, 4)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("完成实名认证后才能创建活动"));
    }

    @Test
    void onlineMeetupDoesNotRequireLocation() throws Exception {
        mockMvc.perform(post("/api/v1/meetups")
                        .with(user(creator.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineBody(topic.getId(), false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetupMode").value("ONLINE"))
                .andExpect(jsonPath("$.data.onlinePlatform").value("英雄联盟"))
                .andExpect(jsonPath("$.data.serverRegion").value("艾欧尼亚"))
                .andExpect(jsonPath("$.data.accessInstructions").value("接受后由创建者发送房间号"))
                .andExpect(jsonPath("$.data.city").doesNotExist())
                .andExpect(jsonPath("$.data.address").doesNotExist());

        Meetup meetup = findMeetup();
        publish(meetup.getId());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}", meetup.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onlinePlatform").value("英雄联盟"))
                .andExpect(jsonPath("$.data.serverRegion").value("艾欧尼亚"))
                .andExpect(jsonPath("$.data.accessInstructions").doesNotExist());

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications", meetup.getId())
                        .with(user(applicant.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"申请加入\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}", meetup.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessInstructions").doesNotExist());

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/applications/{accountId}/accept",
                        meetup.getId(), applicant.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/meetups/{meetupId}", meetup.getId())
                        .with(user(applicant.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessInstructions").value("接受后由创建者发送房间号"));

        mockMvc.perform(post("/api/v1/meetups/{meetupId}/confirm", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());
        Meetup confirmed = meetupMapper.selectById(meetup.getId());
        LocalDateTime now = LocalDateTime.now();
        confirmed.setApplicationDeadline(now.minusHours(3));
        confirmed.setStartTime(now.minusHours(2));
        confirmed.setEndTime(now.minusHours(1));
        assertThat(meetupMapper.updateById(confirmed)).isEqualTo(1);
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/complete", meetup.getId())
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        assertThat(fulfillmentMapper.selectCount(new LambdaQueryWrapper<MeetupFulfillment>()
                .eq(MeetupFulfillment::getMeetupId, meetup.getId())))
                .isZero();
    }

    @Test
    void offlineMeetupRequiresLocation() throws Exception {
        mockMvc.perform(post("/api/v1/meetups")
                        .with(user(creator.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(topic.getId(), null, 4)
                                .replace("\"locationName\":\"滨江体育馆\",", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message")
                        .value("线下活动必须填写地点信息和经纬度"));
    }

    @Test
    void onlineMeetupRejectsOfflineLocation() throws Exception {
        mockMvc.perform(post("/api/v1/meetups")
                        .with(user(creator.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineBody(topic.getId(), true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("线上活动不能填写线下地点信息"));
    }

    @Test
    void onlineMeetupRequiresPlatformAndAccessInstructions() throws Exception {
        mockMvc.perform(post("/api/v1/meetups")
                        .with(user(creator.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(topic.getId(), null, 4)
                                .replace("\"meetupMode\":\"OFFLINE\"", "\"meetupMode\":\"ONLINE\"")
                                .replace("\"city\":\"杭州\",\"district\":\"滨江\",", "")
                                .replace("\"locationName\":\"滨江体育馆\",\"address\":\"滨江区网商路 1 号\",", "")
                                .replace("\"locationLatitude\":30.2084,\"locationLongitude\":120.2123,", "")
                                .replace("\"checkInRadiusMeters\":300,", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("线上活动必须填写线上平台和加入说明"));
    }

    @Test
    void contentAdminCanTerminatePublishedMeetup() throws Exception {
        createMeetup(topic.getId(), null, 4).andExpect(status().isOk());
        Meetup meetup = findMeetup();
        publish(meetup.getId());
        mockMvc.perform(post("/api/v1/admin/meetups/{meetupId}/terminate", meetup.getId())
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"活动内容违规\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TERMINATED"));
        assertThat(auditMapper.selectCount(null)).isEqualTo(3);
    }

    private UserAccount verifiedAccount(String phone, String name, String identityNumber) {
        UserAccount account = accountService.create(phone);
        identityService.submit(account.getId(), name, identityNumber);
        return account;
    }

    private ResultActions createMeetup(Long topicId, Long circleId,
            int capacity) throws Exception {
        return mockMvc.perform(post("/api/v1/meetups")
                .with(user(creator.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(topicId, circleId, capacity)));
    }

    private void publish(long meetupId) throws Exception {
        mockMvc.perform(post("/api/v1/meetups/{meetupId}/publish", meetupId)
                        .with(user(creator.getId().toString())))
                .andExpect(status().isOk());
    }

    private Meetup findMeetup() {
        return meetupMapper.selectOne(new LambdaQueryWrapper<Meetup>()
                .eq(Meetup::getCreatorAccountId, creator.getId()));
    }

    private String body(Long topicId, Long circleId, int capacity) {
        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime end = start.plusHours(2);
        LocalDateTime deadline = start.minusHours(2);
        String association = topicId == null ? "" : "\"topicId\":" + topicId + ",";
        association += circleId == null ? "" : "\"circleId\":" + circleId + ",";
        return "{" + association
                + "\"meetupMode\":\"OFFLINE\","
                + "\"title\":\"周末网球活动\","
                + "\"description\":\"一起打网球\","
                + "\"startTime\":\"" + FORMATTER.format(start) + "\","
                + "\"endTime\":\"" + FORMATTER.format(end) + "\","
                + "\"applicationDeadline\":\"" + FORMATTER.format(deadline) + "\","
                + "\"city\":\"杭州\",\"district\":\"滨江\","
                + "\"locationName\":\"滨江体育馆\",\"address\":\"滨江区网商路 1 号\","
                + "\"locationLatitude\":30.2084,\"locationLongitude\":120.2123,"
                + "\"checkInRadiusMeters\":300,"
                + "\"capacity\":" + capacity + ",\"minimumAge\":18,\"maximumAge\":80,"
                + "\"genderRequirement\":\"ANY\",\"skillRequirement\":\"入门以上\"}";
    }

    private String onlineBody(Long topicId, boolean keepOfflineLocation) {
        String body = body(topicId, null, 5)
                .replace("\"meetupMode\":\"OFFLINE\"", "\"meetupMode\":\"ONLINE\"")
                .replace("\"capacity\":5,", "\"onlinePlatform\":\"英雄联盟\","
                        + "\"serverRegion\":\"艾欧尼亚\","
                        + "\"accessInstructions\":\"接受后由创建者发送房间号\","
                        + "\"capacity\":5,");
        if (keepOfflineLocation) {
            return body;
        }
        return body.replace("\"city\":\"杭州\",\"district\":\"滨江\",", "")
                .replace("\"locationName\":\"滨江体育馆\",\"address\":\"滨江区网商路 1 号\",", "")
                .replace("\"locationLatitude\":30.2084,\"locationLongitude\":120.2123,", "")
                .replace("\"checkInRadiusMeters\":300,", "");
    }
}
