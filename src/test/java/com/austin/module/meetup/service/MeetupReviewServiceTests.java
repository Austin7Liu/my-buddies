package com.austin.module.meetup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.domain.MeetupReviewStatus;
import com.austin.module.meetup.mapper.MeetupFulfillmentMapper;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.meetup.mapper.MeetupReviewAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupReviewMapper;
import com.austin.module.notification.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetupReviewServiceTests {

    @Mock
    private MeetupMapper meetupMapper;

    @Mock
    private MeetupFulfillmentMapper fulfillmentMapper;

    @Mock
    private MeetupReviewMapper reviewMapper;

    @Mock
    private MeetupReviewAuditLogMapper auditMapper;

    @Mock
    private NotificationService notificationService;

    @Test
    void listsReviewsForAdminWithPagination() {
        MeetupReviewService service = new MeetupReviewService(
                meetupMapper,
                fulfillmentMapper,
                reviewMapper,
                auditMapper,
                Clock.systemUTC(),
                notificationService);
        Page<MeetupReview> expected = new Page<>(3, 20, 1);
        expected.setRecords(java.util.List.of(MeetupReview.builder().id(9L).build()));
        when(reviewMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(expected);

        IPage<MeetupReview> result = service.listForAdmin(
                MeetupReviewStatus.HIDDEN, 10L, 11L, 12L, 1, 3, 20);

        assertThat(result.getCurrent()).isEqualTo(3);
        assertThat(result.getRecords()).extracting(MeetupReview::getId).containsExactly(9L);
        verify(reviewMapper).selectPage(any(IPage.class), any(Wrapper.class));
    }
}
