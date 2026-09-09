package com.austin.module.notification.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_notification")
public class UserNotification {

    @TableId
    private Long id;

    private Long recipientAccountId;

    private NotificationType notificationType;

    private String title;

    private String content;

    private NotificationReferenceType referenceType;

    private Long referenceId;

    private String businessKey;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
