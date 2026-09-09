package com.austin.module.risk.domain;

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
@TableName("account_business_restriction")
public class AccountBusinessRestriction {

    @TableId
    private Long id;

    private Long accountId;

    private RestrictionType restrictionType;

    private RestrictionStatus status;

    private String reason;

    private LocalDateTime startsAt;

    private LocalDateTime expiresAt;

    private Long createdBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime revokedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long revokedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String revokeReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer activeMarker;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
