package com.austin.module.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.domain.TopicFollow;
import com.austin.module.catalog.mapper.TopicFollowMapper;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.service.CircleMembershipService;
import com.austin.module.circle.service.CircleService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.service.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TopicFollowControllerTests {

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
    private CircleMembershipService membershipService;

    @Autowired
    private PostService postService;

    @Autowired
    private TopicFollowMapper followMapper;

    private UserAccount follower;
    private UserAccount author;
    private UserAccount moderator;
    private Topic followedTopic;

    @BeforeEach
    void setUp() {
        follower = accountService.create("13900139100");
        author = accountService.create("13900139101");
        moderator = accountService.create("13900139102");
        identityService.submit(author.getId(), "信息流作者", "11010519491231002X");
        followedTopic = catalogService.createTopic(
                moderator.getId(), 101, "follow-test-topic", "关注测试话题", null, 1);
    }

    @Test
    void followAndUnfollowAreIdempotent() throws Exception {
        follow().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByMe").value(true));
        follow().andExpect(status().isOk());
        assertThat(followMapper.selectCount(null)).isEqualTo(1);

        unfollow().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByMe").value(false));
        unfollow().andExpect(status().isOk());
        assertThat(followMapper.selectCount(null)).isZero();
    }

    @Test
    void topicResponseAndMineListContainPersonalFollowState() throws Exception {
        follow();

        mockMvc.perform(get("/api/v1/topics/{topicId}", followedTopic.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByMe").value(false));
        mockMvc.perform(get("/api/v1/topics/{topicId}", followedTopic.getId())
                        .with(user(follower.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByMe").value(true));
        mockMvc.perform(get("/api/v1/me/followed-topics")
                        .with(user(follower.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void personalFeedCombinesFollowedTopicAndJoinedCircleWithoutDuplicates() throws Exception {
        follow();
        Post topicPost = publish("关注话题帖子", followedTopic.getId(), null);
        Circle circle = circleService.create(
                author.getId(), followedTopic.getId(), "关注测试圈子", null, "杭州", "滨江");
        circleService.approve(moderator.getId(), circle.getId());
        membershipService.join(follower.getId(), circle.getId());
        Post circlePost = publish("加入圈子帖子", followedTopic.getId(), circle.getId());

        mockMvc.perform(get("/api/v1/me/post-feed")
                        .with(user(follower.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].likedByMe").value(false));
        assertThat(topicPost.getId()).isNotEqualTo(circlePost.getId());
    }

    @Test
    void disabledTopicIsHiddenButFollowRelationshipIsPreserved() throws Exception {
        follow();
        catalogService.setTopicEnabled(moderator.getId(), followedTopic.getId(), false);

        mockMvc.perform(get("/api/v1/me/followed-topics")
                        .with(user(follower.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        assertThat(followMapper.selectCount(new LambdaQueryWrapper<TopicFollow>()
                .eq(TopicFollow::getAccountId, follower.getId())
                .eq(TopicFollow::getTopicId, followedTopic.getId())))
                .isEqualTo(1);
    }

    @Test
    void nonActiveAccountCannotFollowTopic() throws Exception {
        accountService.requestCancellation(follower.getId());

        follow().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("账户状态不允许操作话题关注"));
    }

    private ResultActions follow() throws Exception {
        return mockMvc.perform(put("/api/v1/topics/{topicId}/follow", followedTopic.getId())
                .with(user(follower.getId().toString())));
    }

    private ResultActions unfollow() throws Exception {
        return mockMvc.perform(delete("/api/v1/topics/{topicId}/follow", followedTopic.getId())
                .with(user(follower.getId().toString())));
    }

    private Post publish(String content, Long topicId, Long circleId) {
        Post post = postService.create(author.getId(), content, topicId, circleId);
        return postService.approve(moderator.getId(), post.getId());
    }
}
