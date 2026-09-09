package com.austin.module.appeal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.common.exception.ForbiddenException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.appeal.domain.ContentAppeal;
import com.austin.module.appeal.domain.ContentAppealAuditLog;
import com.austin.module.appeal.domain.ContentAppealStatus;
import com.austin.module.appeal.mapper.ContentAppealAuditLogMapper;
import com.austin.module.appeal.mapper.ContentAppealMapper;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.post.service.PostCommentService;
import com.austin.module.post.service.PostService;
import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
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
class ContentAppealServiceTests {

    @Mock private ContentAppealMapper appealMapper;
    @Mock private ContentAppealAuditLogMapper auditMapper;
    @Mock private ContentReportMapper reportMapper;
    @Mock private PostMapper postMapper;
    @Mock private PostCommentMapper commentMapper;
    @Mock private PostService postService;
    @Mock private PostCommentService commentService;
    @Mock private UserAccountService accountService;
    @Mock private NotificationService notificationService;

    private ContentAppealService service;

    @BeforeEach
    void setUp() {
        service = new ContentAppealService(appealMapper, auditMapper, reportMapper, postMapper,
                commentMapper, postService, commentService, accountService, notificationService,
                Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void contentAuthorCanAppealResolvedReport() {
        allowActive(1L);
        when(reportMapper.selectById(10L)).thenReturn(resolvedPostReport());
        when(postMapper.selectById(20L)).thenReturn(offlinePost());

        ContentAppeal appeal = service.create(1L, 10L, "处置有误");

        assertThat(appeal.getStatus()).isEqualTo(ContentAppealStatus.PENDING);
        assertThat(appeal.getReason()).isEqualTo("处置有误");
    }

    @Test
    void nonAuthorCannotAppeal() {
        allowActive(2L);
        when(reportMapper.selectById(10L)).thenReturn(resolvedPostReport());
        when(postMapper.selectById(20L)).thenReturn(offlinePost());

        assertThatThrownBy(() -> service.create(2L, 10L, "我要申诉"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approvingAppealRestoresContentAndAudits() {
        ContentAppeal appeal = ContentAppeal.builder().id(30L).reportId(10L)
                .appellantAccountId(1L).status(ContentAppealStatus.PENDING).version(0).build();
        when(appealMapper.selectById(30L)).thenReturn(appeal);
        when(reportMapper.selectById(10L)).thenReturn(resolvedPostReport());
        when(postMapper.selectById(20L)).thenReturn(offlinePost());
        when(appealMapper.updateById(appeal)).thenReturn(1);

        ContentAppeal result = service.approve(99L, 30L, "复核通过");

        assertThat(result.getStatus()).isEqualTo(ContentAppealStatus.APPROVED);
        verify(postService).restore(99L, 20L);
        verify(auditMapper).insert(any(ContentAppealAuditLog.class));
        verify(notificationService).notify(any(Long.class), any(), any(), any(), any(),
                any(Long.class), any());
    }

    private ContentReport resolvedPostReport() {
        return ContentReport.builder().id(10L).targetType(ReportTargetType.POST)
                .reportedPostId(20L).status(ReportStatus.RESOLVED).build();
    }

    private Post offlinePost() {
        return Post.builder().id(20L).authorAccountId(1L).status(PostStatus.OFFLINE).build();
    }

    private void allowActive(long accountId) {
        when(accountService.getById(accountId)).thenReturn(UserAccount.builder()
                .id(accountId).accountStatus(AccountStatus.ACTIVE).build());
    }
}
