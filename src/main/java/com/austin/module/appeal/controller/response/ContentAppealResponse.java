package com.austin.module.appeal.controller.response;

import com.austin.module.appeal.domain.ContentAppeal;
import com.austin.module.appeal.domain.ContentAppealStatus;
import java.time.LocalDateTime;

public record ContentAppealResponse(
        Long id,
        Long reportId,
        String reason,
        ContentAppealStatus status,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime createdAt) {

    public static ContentAppealResponse from(ContentAppeal appeal) {
        return new ContentAppealResponse(appeal.getId(), appeal.getReportId(), appeal.getReason(),
                appeal.getStatus(), appeal.getReviewedAt(), appeal.getReviewNote(),
                appeal.getCreatedAt());
    }
}
