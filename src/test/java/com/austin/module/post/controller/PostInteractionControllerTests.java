package com.austin.module.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.domain.UserNotification;
import com.austin.module.notification.mapper.UserNotificationMapper;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostBookmark;
import com.austin.module.post.domain.PostLike;
import com.austin.module.post.mapper.PostBookmarkMapper;
import com.austin.module.post.mapper.PostLikeMapper;
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
class PostInteractionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService accountService;

    @Autowired
    private IdentityVerificationService identityService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostLikeMapper likeMapper;

    @Autowired
    private PostBookmarkMapper bookmarkMapper;

    @Autowired
    private UserNotificationMapper notificationMapper;

    private UserAccount author;
    private UserAccount visitor;
    private UserAccount moderator;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        author = accountService.create("13900139200");
        visitor = accountService.create("13900139201");
        moderator = accountService.create("13900139202");
        identityService.submit(author.getId(), "互动作者", "11010519491231002X");
        publishedPost = postService.create(author.getId(), "可以互动的帖子", null, null);
        postService.approve(moderator.getId(), publishedPost.getId());
    }

    @Test
    void likeAndUnlikeAreIdempotentAndSummaryIsVisible() throws Exception {
        likeAsVisitor().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));
        likeAsVisitor().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(get("/api/v1/posts/{postId}", publishedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(false));

        unlikeAsVisitor().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0));
        unlikeAsVisitor().andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0));
        assertThat(likeMapper.selectCount(null)).isZero();
    }

    @Test
    void likingAnotherUsersPostCreatesOnlyOneNotification() throws Exception {
        likeAsVisitor();
        likeAsVisitor();

        assertThat(notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getRecipientAccountId, author.getId())
                .eq(UserNotification::getNotificationType, NotificationType.POST_LIKED)))
                .isEqualTo(1);
    }

    @Test
    void likingOwnPostDoesNotCreateNotification() throws Exception {
        mockMvc.perform(put("/api/v1/posts/{postId}/like", publishedPost.getId())
                        .with(user(author.getId().toString())))
                .andExpect(status().isOk());

        assertThat(notificationMapper.selectCount(null)).isZero();
    }

    @Test
    void bookmarkIsPrivateAndHiddenPostDisappearsFromList() throws Exception {
        mockMvc.perform(put("/api/v1/posts/{postId}/bookmark", publishedPost.getId())
                        .with(user(visitor.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarkedByMe").value(true));

        mockMvc.perform(get("/api/v1/me/bookmarked-posts")
                        .with(user(visitor.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/api/v1/me/bookmarked-posts")
                        .with(user(author.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        postService.offline(moderator.getId(), publishedPost.getId(), "测试下架");
        mockMvc.perform(get("/api/v1/me/bookmarked-posts")
                        .with(user(visitor.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        assertThat(bookmarkMapper.selectCount(new LambdaQueryWrapper<PostBookmark>()
                .eq(PostBookmark::getPostId, publishedPost.getId())))
                .isEqualTo(1);
    }

    @Test
    void pendingPostCannotBeLikedOrBookmarked() throws Exception {
        Post pending = postService.create(author.getId(), "待审核帖子", null, null);

        mockMvc.perform(put("/api/v1/posts/{postId}/like", pending.getId())
                        .with(user(visitor.getId().toString())))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/posts/{postId}/bookmark", pending.getId())
                        .with(user(visitor.getId().toString())))
                .andExpect(status().isNotFound());
    }

    private ResultActions likeAsVisitor() throws Exception {
        return mockMvc.perform(put("/api/v1/posts/{postId}/like", publishedPost.getId())
                .with(user(visitor.getId().toString())));
    }

    private ResultActions unlikeAsVisitor() throws Exception {
        return mockMvc.perform(delete("/api/v1/posts/{postId}/like", publishedPost.getId())
                .with(user(visitor.getId().toString())));
    }
}
