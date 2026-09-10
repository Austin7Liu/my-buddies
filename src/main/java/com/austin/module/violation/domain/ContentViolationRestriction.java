package com.austin.module.violation.domain;

import com.austin.module.risk.domain.RestrictionType;
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
@TableName("content_violation_restriction")
public class ContentViolationRestriction {

    @TableId
    private Long violationId;

    private Long restrictionId;

    private RestrictionType restrictionType;

    private LocalDateTime createdAt;
}
