package com.austin.module.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.identity.policy.AgeEligibilityPolicy;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentAuditLog;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.post.mapper.PostCommentAuditLogMapper;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.risk.service.RiskRestrictionService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTests {

    private static final long AUTHOR_ID = 1L;
    private static final long COMMENTER_ID = 2L;
    private static final long POST_ID = 10L;

    @Mock
    private PostCommentMapper commentMapper;

    @Mock
    private PostCommentAuditLogMapper auditLogMapper;

    @Mock
    private PostService postService;

    @Mock
    private UserAccountService accountService;

    @Mock
    private IdentityVerificationService identityService;

    @Mock
    private AgeEligibilityPolicy ageEligibilityPolicy;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RiskRestrictionService restrictionService;

    private PostCommentService service;

    @BeforeEach
    void setUp() {
        service = new PostCommentService(commentMapper, auditLogMapper, postService, accountService,
                identityService, ageEligibilityPolicy, notificationService, restrictionService,
                Clock.fixed(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsCommentAndNotifiesPostAuthor() {
        allowCommenting(COMMENTER_ID);
        when(postService.getPublic(POST_ID)).thenReturn(Post.builder()
                .id(POST_ID)
                .authorAccountId(AUTHOR_ID)
                .build());
        when(commentMapper.insert(any(PostComment.class))).thenAnswer(invocation -> {
            invocation.<PostComment>getArgument(0).setId(100L);
            return 1;
        });

        PostComment result = service.create(COMMENTER_ID, POST_ID, null, "  一起去打球  ");

        assertThat(result.getContent()).isEqualTo("一起去打球");
        assertThat(result.getStatus()).isEqualTo(PostCommentStatus.VISIBLE);
        verify(notificationService).notify(eq(AUTHOR_ID), eq(NotificationType.POST_COMMENTED),
                any(), any(), eq(NotificationReferenceType.POST_COMMENT), eq(100L), any());
    }

    @Test
    void rejectsSecondLevelReply() {
        allowCommenting(COMMENTER_ID);
        when(postService.getPublic(POST_ID)).thenReturn(Post.builder()
                .id(POST_ID)
                .authorAccountId(AUTHOR_ID)
                .build());
        when(commentMapper.selectById(30L)).thenReturn(PostComment.builder()
                .id(30L)
                .postId(POST_ID)
                .parentCommentId(20L)
                .status(PostCommentStatus.VISIBLE)
                .build());

        assertThatThrownBy(() -> service.create(COMMENTER_ID, POST_ID, 30L, "二级回复"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void softDeletesOwnedCommentAndKeepsContent() {
        PostComment comment = PostComment.builder()
                .id(100L)
                .postId(POST_ID)
                .authorAccountId(COMMENTER_ID)
                .content("原始内容")
                .status(PostCommentStatus.VISIBLE)
                .version(0)
                .build();
        when(commentMapper.selectById(100L)).thenReturn(comment);
        when(commentMapper.updateById(comment)).thenReturn(1);

        PostComment result = service.delete(COMMENTER_ID, POST_ID, 100L);

        assertThat(result.getStatus()).isEqualTo(PostCommentStatus.DELETED_BY_AUTHOR);
        assertThat(result.getContent()).isEqualTo("原始内容");
        assertThat(result.getDeletedAt()).isNotNull();
    }

    @Test
    void hidesVisibleCommentAndWritesAuditLog() {
        PostComment comment = PostComment.builder()
                .id(100L)
                .postId(POST_ID)
                .authorAccountId(COMMENTER_ID)
                .status(PostCommentStatus.VISIBLE)
                .version(0)
                .build();
        when(commentMapper.selectById(100L)).thenReturn(comment);
        when(commentMapper.updateById(comment)).thenReturn(1);

        PostComment result = service.hide(AUTHOR_ID, 100L, "  违规内容  ");

        assertThat(result.getStatus()).isEqualTo(PostCommentStatus.HIDDEN_BY_ADMIN);
        ArgumentCaptor<PostCommentAuditLog> captor = ArgumentCaptor.forClass(PostCommentAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("违规内容");
    }

    private void allowCommenting(long accountId) {
        when(accountService.getById(accountId)).thenReturn(UserAccount.builder()
                .id(accountId)
                .accountStatus(AccountStatus.ACTIVE)
                .build());
        var identity = IdentityVerification.builder()
                .accountId(accountId)
                .status(IdentityStatus.VERIFIED)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        when(identityService.findByAccountId(accountId)).thenReturn(identity);
        when(ageEligibilityPolicy.isAdult(identity.getBirthDate())).thenReturn(true);
    }
}
