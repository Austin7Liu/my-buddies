package com.austin.module.circle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleStatus;
import com.austin.module.circle.mapper.CircleAuditLogMapper;
import com.austin.module.circle.mapper.CircleMapper;
import com.austin.module.identity.service.IdentityVerificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CircleControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountService accountService;
    @Autowired private IdentityVerificationService identityService;
    @Autowired private CatalogService catalogService;
    @Autowired private CircleMapper circleMapper;
    @Autowired private CircleAuditLogMapper auditMapper;
    private UserAccount creator;
    private UserAccount reviewer;
    private Topic topic;

    @BeforeEach
    void setUp() {
        creator = accountService.create("13900139400");
        reviewer = accountService.create("13900139401");
        identityService.submit(creator.getId(), "测试用户", "11010519491231002X");
        topic = catalogService.createTopic(reviewer.getId(), 101, "circle-test-topic", "圈子测试话题", null, 1);
    }

    @Test
    void circleIsHiddenUntilContentAdminApprovesIt() throws Exception {
        createCircle("杭州滨江网球").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
        Circle circle = findCreatorCircle();

        mockMvc.perform(get("/api/v1/topics/{topicId}/circles", topic.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(post("/api/v1/admin/circles/{circleId}/approve", circle.getId())
                        .with(user(reviewer.getId().toString()).roles("CONTENT_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(get("/api/v1/topics/{topicId}/circles", topic.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        assertThat(auditMapper.selectCount(null)).isEqualTo(2);
    }

    @Test
    void sameTopicCannotContainDuplicateCircleName() throws Exception {
        createCircle("杭州滨江网球").andExpect(status().isOk());
        UserAccount another = accountService.create("13900139402");
        identityService.submit(another.getId(), "另一用户", "110105198806150016");
        mockMvc.perform(post("/api/v1/circles").with(user(another.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(body("杭州滨江网球")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("该话题下已存在同名圈子"));
    }

    @Test
    void unverifiedUserCannotCreateCircle() throws Exception {
        UserAccount unverified = accountService.create("13900139403");
        mockMvc.perform(post("/api/v1/circles").with(user(unverified.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(body("杭州西湖网球")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("完成实名认证后才能创建圈子"));
    }

    @Test
    void rejectedCircleCanBeEditedAndResubmitted() throws Exception {
        createCircle("杭州滨江网球").andExpect(status().isOk()); Circle circle = findCreatorCircle();
        mockMvc.perform(post("/api/v1/admin/circles/{circleId}/reject", circle.getId())
                        .with(user(reviewer.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"名称需要更具体\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));
        mockMvc.perform(put("/api/v1/circles/{circleId}", circle.getId())
                        .with(user(creator.getId().toString())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"杭州滨江周末网球\",\"description\":\"周末网球活动\",\"city\":\"杭州\",\"district\":\"滨江\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.rejectionReason").doesNotExist());
        assertThat(circleMapper.selectById(circle.getId()).getStatus()).isEqualTo(CircleStatus.PENDING_REVIEW);
    }

    private org.springframework.test.web.servlet.ResultActions createCircle(String name) throws Exception {
        return mockMvc.perform(post("/api/v1/circles").with(user(creator.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON).content(body(name)));
    }

    private String body(String name) {
        return "{\"topicId\":" + topic.getId() + ",\"name\":\"" + name
                + "\",\"description\":\"网球活动\",\"city\":\"杭州\",\"district\":\"滨江\"}";
    }

    private Circle findCreatorCircle() {
        return circleMapper.selectOne(new LambdaQueryWrapper<Circle>()
                .eq(Circle::getCreatorAccountId, creator.getId()));
    }
}

