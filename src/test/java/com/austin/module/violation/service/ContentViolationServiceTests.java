package com.austin.module.violation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.appeal.domain.ContentAppeal;
import com.austin.module.appeal.domain.ContentAppealStatus;
import com.austin.module.appeal.mapper.ContentAppealMapper;
import com.austin.module.notification.service.NotificationService;
import com.austin.module.post.domain.Post;
import com.austin.module.post.mapper.PostCommentMapper;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.report.domain.ContentReport;
import com.austin.module.report.domain.ReportStatus;
import com.austin.module.report.domain.ReportTargetType;
import com.austin.module.report.mapper.ContentReportMapper;
import com.austin.module.risk.domain.AccountBusinessRestriction;
import com.austin.module.risk.domain.RestrictionType;
import com.austin.module.risk.service.RiskRestrictionService;
import com.austin.module.violation.domain.AccountContentViolation;
import com.austin.module.violation.domain.ContentViolationRestriction;
import com.austin.module.violation.domain.ViolationPenaltyType;
import com.austin.module.violation.domain.ViolationSeverity;
import com.austin.module.violation.mapper.AccountContentViolationAuditLogMapper;
import com.austin.module.violation.mapper.AccountContentViolationMapper;
import com.austin.module.violation.mapper.ContentViolationRestrictionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentViolationServiceTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 10, 0);

    @Mock
    private AccountContentViolationMapper violationMapper;

    @Mock
    private AccountContentViolationAuditLogMapper auditMapper;

    @Mock
    private ContentViolationRestrictionMapper linkMapper;

    @Mock
    private ContentReportMapper reportMapper;

    @Mock
    private ContentAppealMapper appealMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private PostCommentMapper commentMapper;

    @Mock
    private RiskRestrictionService restrictionService;

    @Mock
    private NotificationService notificationService;

    private ContentViolationService service;

    @BeforeEach
    void setUp() {
        service = new ContentViolationService(violationMapper, auditMapper, linkMapper, reportMapper,
                appealMapper, postMapper, commentMapper, restrictionService, notificationService,
                Clock.fixed(Instant.parse("2026-09-10T02:00:00Z"), ZoneOffset.UTC));
        lenient().when(linkMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void rejectsClosedAppealAndActiveAppealWindow() {
        ContentReport report = resolvedReport(NOW.minusDays(8));
        when(reportMapper.selectById(10L)).thenReturn(report);
        when(appealMapper.selectOne(any())).thenReturn(ContentAppeal.builder()
                .status(ContentAppealStatus.CLOSED)
                .build());

        assertThatThrownBy(() -> confirmWarning(10L)).isInstanceOf(ConflictException.class);

        when(appealMapper.selectOne(any())).thenReturn(null);
        report.setAppealDeadlineAt(NOW.plusDays(1));
        assertThatThrownBy(() -> confirmWarning(10L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmsWarningAfterRejectedAppealWithoutRestriction() {
        when(reportMapper.selectById(10L)).thenReturn(resolvedReport(NOW.plusDays(1)));
        when(appealMapper.selectOne(any())).thenReturn(ContentAppeal.builder()
                .status(ContentAppealStatus.REJECTED)
                .build());
        when(postMapper.selectById(20L)).thenReturn(Post.builder()
                .id(20L)
                .authorAccountId(1L)
                .build());
        when(violationMapper.insert(any(AccountContentViolation.class))).thenAnswer(invocation -> {
            invocation.<AccountContentViolation>getArgument(0).setId(30L);
            return 1;
        });

        var view = confirmWarning(10L);

        assertThat(view.violation().getAccountId()).isEqualTo(1L);
        verify(restrictionService, never()).create(any(Long.class), any(Long.class),
                any(), any(), any());
    }

    @Test
    void createsAndLinksBothContentRestrictions() {
        when(reportMapper.selectById(10L)).thenReturn(resolvedReport(NOW.minusDays(8)));
        when(postMapper.selectById(20L)).thenReturn(Post.builder().authorAccountId(1L).build());
        when(violationMapper.insert(any(AccountContentViolation.class))).thenAnswer(invocation -> {
            invocation.<AccountContentViolation>getArgument(0).setId(30L);
            return 1;
        });
        when(restrictionService.create(any(Long.class), any(Long.class), any(), any(), any()))
                .thenReturn(AccountBusinessRestriction.builder()
                                .id(40L)
                                .restrictionType(RestrictionType.POST_CREATE_DISABLED)
                                .build(),
                        AccountBusinessRestriction.builder()
                                .id(41L)
                                .restrictionType(RestrictionType.COMMENT_CREATE_DISABLED)
                                .build());

        service.confirm(99L, 10L, ViolationSeverity.MODERATE,
                ViolationPenaltyType.CONTENT_CREATE_DISABLED, NOW.plusDays(7), "限制发布");

        verify(restrictionService, times(2)).create(any(Long.class), any(Long.class),
                any(), any(), any());
        verify(linkMapper, times(2)).insert(any(ContentViolationRestriction.class));
    }

    @Test
    void hidesAnotherUsersViolationAsNotFound() {
        when(violationMapper.selectById(30L)).thenReturn(AccountContentViolation.builder()
                .id(30L)
                .accountId(1L)
                .build());

        assertThatThrownBy(() -> service.getMine(2L, 30L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ContentViolationService.ViolationView confirmWarning(long reportId) {
        return service.confirm(99L, reportId, ViolationSeverity.MINOR,
                ViolationPenaltyType.WARNING_ONLY, null, "首次警告");
    }

    private ContentReport resolvedReport(LocalDateTime deadline) {
        return ContentReport.builder()
                .id(10L)
                .targetType(ReportTargetType.POST)
                .reportedPostId(20L)
                .status(ReportStatus.RESOLVED)
                .appealDeadlineAt(deadline)
                .build();
    }
}
