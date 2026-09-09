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
@TableName("post_comment_audit_log")
public class PostCommentAuditLog {

    @TableId
    private Long id;

    private Long commentId;

    private Long operatorAccountId;

    private PostCommentAuditAction action;

    private String reason;

    private LocalDateTime occurredAt;
}
