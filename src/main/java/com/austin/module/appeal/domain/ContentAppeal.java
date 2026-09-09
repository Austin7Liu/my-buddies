package com.austin.module.appeal.domain;

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
@TableName("content_appeal")
public class ContentAppeal {

    @TableId
    private Long id;

    private Long reportId;

    private Long appellantAccountId;

    private String reason;

    private ContentAppealStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reviewedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime reviewedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reviewNote;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
