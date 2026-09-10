package com.austin.module.violation.controller.response;

import com.austin.module.violation.domain.AccountContentViolation;
import com.austin.module.violation.domain.ViolationPenaltyType;
import com.austin.module.violation.domain.ViolationSeverity;
import com.austin.module.violation.domain.ViolationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ContentViolationResponse(
        Long id,
        Long accountId,
        Long reportId,
        ViolationSeverity severity,
        ViolationPenaltyType penaltyType,
        LocalDateTime penaltyExpiresAt,
        String note,
        ViolationStatus status,
        LocalDateTime confirmedAt,
        LocalDateTime revokedAt,
        String revokeReason,
        List<Long> linkedRestrictionIds) {

    public static ContentViolationResponse from(AccountContentViolation value) {
        return from(value, List.of());
    }

    public static ContentViolationResponse from(AccountContentViolation value,
            List<Long> linkedRestrictionIds) {
        return new ContentViolationResponse(value.getId(), value.getAccountId(), value.getReportId(),
                value.getSeverity(), value.getPenaltyType(), value.getPenaltyExpiresAt(), value.getNote(),
                value.getStatus(), value.getConfirmedAt(), value.getRevokedAt(), value.getRevokeReason(),
                linkedRestrictionIds);
    }
}
