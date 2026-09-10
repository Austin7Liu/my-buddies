package com.austin.module.violation.domain;

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
@TableName("account_content_violation")
public class AccountContentViolation {

    @TableId
    private Long id;

    private Long accountId;

    private Long reportId;

    private ViolationSeverity severity;

    private ViolationPenaltyType penaltyType;

    private LocalDateTime penaltyExpiresAt;

    private String note;

    private ViolationStatus status;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long revokedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime revokedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String revokeReason;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
