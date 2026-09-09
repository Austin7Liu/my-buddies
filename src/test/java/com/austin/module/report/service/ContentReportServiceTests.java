package com.austin.module.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.common.exception.ConflictException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.post.service.PostCommentService;
import com.austin.module.post.service.PostService;
import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ContentReportAuditLog;
import com.austin.module.report.domain.ReportReasonType;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
import com.austin.module.report.mapper.ContentReportAuditLogMapper;
import com.austin.module.report.mapper.ContentReportMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentReportServiceTests {

    @Mock
    private ContentReportMapper reportMapper;

    @Mock
    private ContentReportAuditLogMapper auditMapper;

    @Mock
    private PostCommentMapper commentMapper;

    @Mock
    private PostService postService;

    @Mock
    private PostCommentService commentService;

    @Mock
    private UserAccountService accountService;

    @Mock
    private NotificationService notificationService;

    private ContentReportService service;

    @BeforeEach
    void setUp() {
        service = new ContentReportService(reportMapper, auditMapper, commentMapper, postService,
                commentService, accountService, notificationService,
                Clock.fixed(Instant.parse("2026-09-09T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void reportsPublicPostWithSnapshot() {
        allowReporter(2L);
        when(postService.getPublic(10L)).thenReturn(Post.builder()
                .id(10L).authorAccountId(1L).content("原帖内容").build());

        ContentReport report = service.reportPost(2L, 10L, ReportReasonType.SPAM, null);

        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.POST);
        assertThat(report.getContentSnapshot()).isEqualTo("原帖内容");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    void refusesSelfReportAndOtherWithoutDescription() {
        allowReporter(2L);
        when(postService.getPublic(10L)).thenReturn(Post.builder()
                .id(10L).authorAccountId(2L).content("内容").build());
        assertThatThrownBy(() -> service.reportPost(2L, 10L, ReportReasonType.SPAM, null))
                .isInstanceOf(ConflictException.class);

        when(postService.getPublic(11L)).thenReturn(Post.builder()
                .id(11L).authorAccountId(1L).content("内容").build());
        assertThatThrownBy(() -> service.reportPost(2L, 11L, ReportReasonType.OTHER, " "))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reportsVisibleComment() {
        allowReporter(2L);
        when(commentMapper.selectById(20L)).thenReturn(PostComment.builder()
                .id(20L).postId(10L).authorAccountId(1L)
                .status(PostCommentStatus.VISIBLE).content("评论快照").build());
        when(postService.getPublic(10L)).thenReturn(Post.builder().id(10L).build());

        ContentReport report = service.reportComment(2L, 20L, ReportReasonType.HARASSMENT, "攻击");

        assertThat(report.getReportedCommentId()).isEqualTo(20L);
        assertThat(report.getContentSnapshot()).isEqualTo("评论快照");
    }

    @Test
    void resolvesCommentReportAndAudits() {
        ContentReport report = ContentReport.builder().id(30L).reporterAccountId(2L)
                .targetType(ReportTargetType.POST_COMMENT).reportedCommentId(20L)
                .status(ReportStatus.PENDING).version(0).build();
        when(reportMapper.selectById(30L)).thenReturn(report);
        when(reportMapper.updateById(report)).thenReturn(1);

        ContentReport result = service.resolve(99L, 30L, "确认违规");

        assertThat(result.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(commentService).hide(99L, 20L, "确认违规");
        verify(auditMapper).insert(any(ContentReportAuditLog.class));
        verify(notificationService).notify(any(Long.class), any(), any(), any(), any(), any(Long.class), any());
    }

    private void allowReporter(long accountId) {
        when(accountService.getById(accountId)).thenReturn(UserAccount.builder()
                .id(accountId).accountStatus(AccountStatus.ACTIVE).build());
    }
}
