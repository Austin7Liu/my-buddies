package com.austin.module.meetup.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.meetup.domain.FulfillmentResult;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupFulfillment;
import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.meetup.domain.MeetupReview;
import com.austin.module.meetup.domain.MeetupReviewAuditAction;
import com.austin.module.meetup.domain.MeetupReviewAuditLog;
import com.austin.module.meetup.domain.MeetupReviewStatus;
import com.austin.module.meetup.domain.MeetupStatus;
import com.austin.module.meetup.mapper.MeetupFulfillmentMapper;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.meetup.mapper.MeetupReviewAuditLogMapper;
import com.austin.module.meetup.mapper.MeetupReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetupReviewService {

    private static final int REVIEW_WINDOW_DAYS = 7;

    private final MeetupMapper meetupMapper;
    private final MeetupFulfillmentMapper fulfillmentMapper;
    private final MeetupReviewMapper reviewMapper;
    private final MeetupReviewAuditLogMapper auditMapper;
    private final Clock clock;

    @Transactional
    public MeetupReview create(long reviewerId, long meetupId, long revieweeId, int rating, String comment) {
        validateRating(rating);
        validateReviewEligibility(reviewerId, revieweeId, requireMeetup(meetupId));
        LocalDateTime now = LocalDateTime.now(clock);
        MeetupReview review = MeetupReview.builder()
                .meetupId(meetupId)
                .reviewerAccountId(reviewerId)
                .revieweeAccountId(revieweeId)
                .rating(rating)
                .comment(normalize(comment))
                .status(MeetupReviewStatus.VISIBLE)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("不能重复评价同一参与者", exception);
        }
        return review;
    }

    @Transactional
    public MeetupReview update(long reviewerId, long meetupId, long reviewId, int rating, String comment) {
        validateRating(rating);
        MeetupReview review = requireReview(reviewId);
        if (!review.getMeetupId().equals(meetupId)) {
            throw new ResourceNotFoundException("评价不存在");
        }
        if (!review.getReviewerAccountId().equals(reviewerId)) {
            throw new ForbiddenException("只能修改自己的评价");
        }
        ensureWithinReviewWindow(requireMeetup(meetupId));
        review.setRating(rating);
        review.setComment(normalize(comment));
        review.setUpdatedAt(LocalDateTime.now(clock));
        if (reviewMapper.updateById(review) != 1) {
            throw new ConflictException("评价已发生变化，请刷新后重试");
        }
        return review;
    }

    @Transactional(readOnly = true)
    public IPage<MeetupReview> listMine(long reviewerId, long meetupId, long page, long size) {
        requireMeetup(meetupId);
        return reviewMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MeetupReview>()
                .eq(MeetupReview::getMeetupId, meetupId)
                .eq(MeetupReview::getReviewerAccountId, reviewerId)
                .orderByDesc(MeetupReview::getCreatedAt));
    }

    @Transactional(readOnly = true)
    public IPage<MeetupReview> listReceived(long accountId, long page, long size) {
        return reviewMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MeetupReview>()
                .eq(MeetupReview::getRevieweeAccountId, accountId)
                .eq(MeetupReview::getStatus, MeetupReviewStatus.VISIBLE)
                .orderByDesc(MeetupReview::getCreatedAt)
                .orderByDesc(MeetupReview::getId));
    }

    @Transactional
    public MeetupReview moderate(long operatorId, long reviewId, MeetupReviewStatus target, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ConflictException("管理评价必须填写原因");
        }
        MeetupReview review = requireReview(reviewId);
        if (review.getStatus() == target) {
            throw new ConflictException("评价状态无需修改");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        review.setStatus(target);
        review.setUpdatedAt(now);
        if (reviewMapper.updateById(review) != 1) {
            throw new ConflictException("评价状态已发生变化，请刷新后重试");
        }
        auditMapper.insert(MeetupReviewAuditLog.builder()
                .reviewId(reviewId)
                .operatorAccountId(operatorId)
                .action(target == MeetupReviewStatus.HIDDEN
                        ? MeetupReviewAuditAction.HIDE : MeetupReviewAuditAction.RESTORE)
                .reason(reason.trim())
                .occurredAt(now)
                .build());
        return review;
    }

    private void validateReviewEligibility(long reviewerId, long revieweeId, Meetup meetup) {
        if (reviewerId == revieweeId) {
            throw new ConflictException("不能评价自己");
        }
        if (meetup.getMeetupMode() != MeetupMode.OFFLINE || meetup.getStatus() != MeetupStatus.COMPLETED) {
            throw new ConflictException("只有已完成的线下活动可以评价");
        }
        ensureWithinReviewWindow(meetup);
        requireAttended(meetup.getId(), reviewerId);
        requireAttended(meetup.getId(), revieweeId);
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new ConflictException("评分必须在 1 到 5 之间");
        }
    }

    private void requireAttended(long meetupId, long accountId) {
        MeetupFulfillment fulfillment = fulfillmentMapper.selectOne(new LambdaQueryWrapper<MeetupFulfillment>()
                .eq(MeetupFulfillment::getMeetupId, meetupId)
                .eq(MeetupFulfillment::getAccountId, accountId));
        if (fulfillment == null || fulfillment.getResult() != FulfillmentResult.ATTENDED) {
            throw new ConflictException("只有实际出席的参与者之间可以互评");
        }
    }

    private void ensureWithinReviewWindow(Meetup meetup) {
        if (meetup.getCompletedAt() == null
                || LocalDateTime.now(clock).isAfter(meetup.getCompletedAt().plusDays(REVIEW_WINDOW_DAYS))) {
            throw new ConflictException("活动评价期已结束");
        }
    }

    private Meetup requireMeetup(long meetupId) {
        Meetup meetup = meetupMapper.selectById(meetupId);
        if (meetup == null) {
            throw new ResourceNotFoundException("活动不存在");
        }
        return meetup;
    }

    private MeetupReview requireReview(long reviewId) {
        MeetupReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new ResourceNotFoundException("评价不存在");
        }
        return review;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
