package com.austin.module.risk.controller.response;

import com.austin.module.risk.domain.AccountBusinessRestriction;
import com.austin.module.risk.domain.RestrictionStatus;
import com.austin.module.risk.domain.RestrictionType;
import java.time.LocalDateTime;

public record RestrictionResponse(
        Long id,

        Long accountId,

        RestrictionType restrictionType,

        RestrictionStatus status,

        String reason,

        LocalDateTime startsAt,

        LocalDateTime expiresAt,

        Long createdBy,

        LocalDateTime revokedAt,

        Long revokedBy,

        String revokeReason,

        boolean effective,

        LocalDateTime createdAt,

        LocalDateTime updatedAt) {

    public static RestrictionResponse from(AccountBusinessRestriction restriction, LocalDateTime now) {
        return new RestrictionResponse(
                restriction.getId(),
                restriction.getAccountId(),
                restriction.getRestrictionType(),
                restriction.getStatus(),
                restriction.getReason(),
                restriction.getStartsAt(),
                restriction.getExpiresAt(),
                restriction.getCreatedBy(),
                restriction.getRevokedAt(),
                restriction.getRevokedBy(),
                restriction.getRevokeReason(),
                restriction.getStatus() == RestrictionStatus.ACTIVE
                        && restriction.getStartsAt().compareTo(now) <= 0
                        && restriction.getExpiresAt().isAfter(now),
                restriction.getCreatedAt(),
                restriction.getUpdatedAt());
    }
}
