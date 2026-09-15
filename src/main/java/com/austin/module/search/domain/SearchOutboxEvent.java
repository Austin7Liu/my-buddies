package com.austin.module.search.domain;

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
@TableName("search_outbox_event")
public class SearchOutboxEvent {

    @TableId
    private Long id;

    private SearchDocumentType aggregateType;

    private Long aggregateId;

    private SearchOutboxEventType eventType;

    private SearchOutboxStatus status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String lastError;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
