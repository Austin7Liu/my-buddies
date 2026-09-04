package com.austin.module.post.domain;

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
@TableName("post_audit_log")
public class PostAuditLog {

    @TableId
    private Long id;

    private Long postId;

    private Long operatorAccountId;

    private PostAuditAction action;

    private PostStatus fromStatus;

    private PostStatus toStatus;

    private String reason;

    private LocalDateTime occurredAt;
}
