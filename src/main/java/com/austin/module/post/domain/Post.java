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
@TableName("post")
public class Post {

    @TableId
    private Long id;

    private Long authorAccountId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long topicId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long circleId;

    private String content;

    private PostStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String moderationReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long moderatedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime moderatedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime deletedAt;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
