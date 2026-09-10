package com.austin.module.report.domain;

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
@TableName("content_report")
public class ContentReport {

    @TableId
    private Long id;

    private Long reporterAccountId;

    private ReportTargetType targetType;

    private Long reportedPostId;

    private Long reportedCommentId;

    private ReportReasonType reasonType;

    private String description;

    private String contentSnapshot;

    private ReportStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long handledBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime handledAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime appealDeadlineAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resolutionNote;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer activeMarker;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
