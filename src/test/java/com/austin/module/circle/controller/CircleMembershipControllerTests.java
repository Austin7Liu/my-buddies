package com.austin.module.circle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleMember;
import com.austin.module.circle.domain.CircleMemberRole;
import com.austin.module.circle.domain.CircleMemberStatus;
import com.austin.module.circle.mapper.CircleMemberMapper;
import com.austin.module.circle.service.CircleService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CircleMembershipControllerTests {

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
    private CircleMemberMapper memberMapper;

    private UserAccount owner;
    private UserAccount member;
    private Circle circle;

    @BeforeEach
    void setUp() {
        owner = accountService.create("13900139300");
        member = accountService.create("13900139301");
        UserAccount reviewer = accountService.create("13900139302");
        identityService.submit(owner.getId(), "圈主用户", "11010519491231002X");
        Topic topic = catalogService.createTopic(
                reviewer.getId(), 101, "membership-test-topic", "成员测试话题", null, 1);
        circle = circleService.create(owner.getId(), topic.getId(), "杭州跑步搭子", null, "杭州", "西湖");
        circleService.approve(reviewer.getId(), circle.getId());
    }

    @Test
    void creatorAutomaticallyBecomesOwner() {
        CircleMember membership = find(owner.getId());

        assertThat(membership.getRole()).isEqualTo(CircleMemberRole.OWNER);
        assertThat(membership.getStatus()).isEqualTo(CircleMemberStatus.ACTIVE);
    }

    @Test
    void memberCanJoinLeaveAndRejoin() throws Exception {
        mockMvc.perform(post("/api/v1/circles/{circleId}/memberships", circle.getId())
                        .with(user(member.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/v1/circles/{circleId}/memberships/me", circle.getId())
                        .with(user(member.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LEFT"));

        mockMvc.perform(post("/api/v1/circles/{circleId}/memberships", circle.getId())
                        .with(user(member.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(memberMapper.selectCount(null)).isEqualTo(2);
    }

    @Test
    void duplicateJoinIsIdempotentAndMemberListIsPublic() throws Exception {
        joinMember();
        joinMember();

        mockMvc.perform(get("/api/v1/circles/{circleId}/members", circle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].account.phone").doesNotExist());
        assertThat(memberMapper.selectCount(null)).isEqualTo(2);
    }

    @Test
    void ownerCannotLeaveCircle() throws Exception {
        mockMvc.perform(delete("/api/v1/circles/{circleId}/memberships/me", circle.getId())
                        .with(user(owner.getId().toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("圈子创建者不能退出自己的圈子"));
    }

    @Test
    void membershipDetailsAndMyCirclesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/circles/{circleId}/memberships/me", circle.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/me/circles")
                        .with(user(owner.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    private void joinMember() throws Exception {
        mockMvc.perform(post("/api/v1/circles/{circleId}/memberships", circle.getId())
                        .with(user(member.getId().toString())))
                .andExpect(status().isOk());
    }

    private CircleMember find(long accountId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circle.getId())
                .eq(CircleMember::getAccountId, accountId));
    }
}
