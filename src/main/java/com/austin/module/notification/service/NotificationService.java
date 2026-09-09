package com.austin.module.notification.service;

import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.domain.UserNotification;
import com.austin.module.notification.mapper.UserNotificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
public class NotificationService {

    private final UserNotificationMapper notificationMapper;
    private final Clock clock;

    @Transactional
    public void notify(long recipientId, NotificationType type, String title, String content,
            NotificationReferenceType referenceType, long referenceId, String businessKey) {
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            notificationMapper.insert(UserNotification.builder()
                    .recipientAccountId(recipientId)
                    .notificationType(type)
                    .title(title)
                    .content(content)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .businessKey(businessKey)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        } catch (DuplicateKeyException ignored) {
            // Stable business keys make repeated business requests notification-idempotent.
        }
    }

    @Transactional(readOnly = true)
    public IPage<UserNotification> listMine(long accountId, boolean unreadOnly, long page, long size) {
        LambdaQueryWrapper<UserNotification> query = new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getRecipientAccountId, accountId)
                .orderByDesc(UserNotification::getCreatedAt)
                .orderByDesc(UserNotification::getId);
        if (unreadOnly) {
            query.isNull(UserNotification::getReadAt);
        }
        return notificationMapper.selectPage(new Page<>(page, size), query);
    }

    @Transactional(readOnly = true)
    public long unreadCount(long accountId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getRecipientAccountId, accountId)
                .isNull(UserNotification::getReadAt));
    }

    @Transactional
    public UserNotification read(long accountId, long notificationId) {
        UserNotification notification = notificationMapper.selectOne(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getRecipientAccountId, accountId));
        if (notification == null) {
            throw new ResourceNotFoundException("通知不存在");
        }
        if (notification.getReadAt() == null) {
            LocalDateTime now = LocalDateTime.now(clock);
            notification.setReadAt(now);
            notification.setUpdatedAt(now);
            notificationMapper.updateById(notification);
        }
        return notification;
    }

    @Transactional
    public long readAll(long accountId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return notificationMapper.update(new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getRecipientAccountId, accountId)
                .isNull(UserNotification::getReadAt)
                .set(UserNotification::getReadAt, now)
                .set(UserNotification::getUpdatedAt, now));
    }
}
