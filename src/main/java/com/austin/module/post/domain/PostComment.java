package com.austin.module.post.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
@TableName("post_comment")
public class PostComment {

    @TableId
    private Long id;

    private Long postId;

    private Long authorAccountId;

    private Long parentCommentId;

    private String content;

    private PostCommentStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime deletedAt;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
