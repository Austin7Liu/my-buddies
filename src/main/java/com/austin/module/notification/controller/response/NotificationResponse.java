package com.austin.module.notification.controller.response;

import com.austin.module.notification.domain.NotificationReferenceType;
import com.austin.module.notification.domain.NotificationType;
import com.austin.module.notification.domain.UserNotification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,

        NotificationType notificationType,

        String title,

        String content,

        NotificationReferenceType referenceType,

        Long referenceId,

        boolean read,

        LocalDateTime readAt,

        LocalDateTime createdAt) {

    public static NotificationResponse from(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getNotificationType(), notification.getTitle(),
                notification.getContent(), notification.getReferenceType(), notification.getReferenceId(),
                notification.getReadAt() != null, notification.getReadAt(), notification.getCreatedAt());
    }
}
