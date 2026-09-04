package com.austin.module.circle.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("circle")
public class Circle {

    @TableId
    private Long id;

    private Long topicId;

    private Long creatorAccountId;

    private String name;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    private String city;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String district;

    private CircleStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rejectionReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reviewedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime reviewedAt;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
