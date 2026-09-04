package com.austin.module.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.austin.module.circle.service.CircleService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.mapper.PostAuditLogMapper;
import com.austin.module.post.mapper.PostMapper;
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
class PostControllerTests {

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
    private PostMapper postMapper;

    @Autowired
    private PostAuditLogMapper auditMapper;

    private UserAccount author;
    private UserAccount moderator;
    private Topic topic;
    private Circle circle;

    @BeforeEach
    void setUp() {
        author = accountService.create("13900139500");
        moderator = accountService.create("13900139501");
        identityService.submit(author.getId(), "帖子用户", "11010519491231002X");
        topic = catalogService.createTopic(moderator.getId(), 101, "post-test-topic", "帖子测试话题", null, 1);
        circle = circleService.create(author.getId(), topic.getId(), "帖子测试圈子", null, "杭州", "滨江");
        circleService.approve(moderator.getId(), circle.getId());
    }

    @Test
    void ordinaryPostHasNoAssociationAndIsHiddenUntilApproved() throws Exception {
        createPost("{\"content\":\"首页普通帖子 #马术\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").doesNotExist())
                .andExpect(jsonPath("$.data.circleId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
        Post value = findAuthorPost();

        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        approve(value.getId());
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void topicPostUsesTopicProvidedByClient() throws Exception {
        createPost("{\"content\":\"网球帖子 #网球\",\"topicId\":" + topic.getId() + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.data.circleId").doesNotExist());
    }

    @Test
    void circlePostAutomaticallyGetsCirclesTopic() throws Exception {
        createPost("{\"content\":\"圈子帖子\",\"circleId\":" + circle.getId() + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.data.circleId").value(circle.getId()));
    }

    @Test
    void mismatchedTopicAndCircleAreRejected() throws Exception {
        Topic another = catalogService.createTopic(moderator.getId(), 101, "post-other-topic", "其他话题", null, 2);
        createPost("{\"content\":\"错误关联\",\"topicId\":" + another.getId()
                + ",\"circleId\":" + circle.getId() + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Circle 与 Topic 不匹配"));
    }

    @Test
    void unverifiedUserCannotCreatePost() throws Exception {
        UserAccount unverified = accountService.create("13900139502");
        mockMvc.perform(post("/api/v1/posts")
                        .with(user(unverified.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"不能发布\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("完成实名认证后才能发布帖子"));
    }

    @Test
    void editingPublishedPostRequiresReviewAgain() throws Exception {
        createPost("{\"content\":\"原始内容\"}").andExpect(status().isOk());
        Post value = findAuthorPost();
        approve(value.getId());

        mockMvc.perform(put("/api/v1/posts/{postId}", value.getId())
                        .with(user(author.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"修改后的内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
        assertThat(postMapper.selectById(value.getId()).getModeratedBy()).isNull();
    }

    @Test
    void authorDeleteAndModeratorOfflineAreDifferentStates() throws Exception {
        createPost("{\"content\":\"作者删除的帖子\"}").andExpect(status().isOk());
        Post deleted = findAuthorPost();
        mockMvc.perform(delete("/api/v1/posts/{postId}", deleted.getId())
                        .with(user(author.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        createPost("{\"content\":\"管理员下架的帖子\"}").andExpect(status().isOk());
        Post offline = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorAccountId, author.getId())
                .eq(Post::getStatus, PostStatus.PENDING_REVIEW));
        approve(offline.getId());
        mockMvc.perform(post("/api/v1/admin/posts/{postId}/offline", offline.getId())
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"内容违规\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));
        assertThat(auditMapper.selectCount(null)).isEqualTo(5);
    }

    private org.springframework.test.web.servlet.ResultActions createPost(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/posts")
                .with(user(author.getId().toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void approve(long postId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/posts/{postId}/approve", postId)
                        .with(user(moderator.getId().toString()).roles("CONTENT_ADMIN")))
                .andExpect(status().isOk());
    }

    private Post findAuthorPost() {
        return postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorAccountId, author.getId()));
    }
}
