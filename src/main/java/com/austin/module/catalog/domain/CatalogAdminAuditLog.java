package com.austin.module.catalog.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("catalog_admin_audit_log")
public class CatalogAdminAuditLog {
    @TableId private Long id;
    private Long operatorAccountId;
    private CatalogEntityType entityType;
    private Long entityId;
    private CatalogAuditAction action;
    private LocalDateTime occurredAt;
}

