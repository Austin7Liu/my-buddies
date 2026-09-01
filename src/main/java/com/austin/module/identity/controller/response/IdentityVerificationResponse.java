package com.austin.module.identity.controller.response;

import com.austin.module.identity.domain.Gender;
import com.austin.module.identity.domain.IdentityStatus;
import com.austin.module.identity.domain.IdentityVerification;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record IdentityVerificationResponse(
        IdentityStatus status,
        LocalDate birthDate,
        Gender gender,
        boolean adult,
        String failureCode,
        LocalDateTime submittedAt,
        LocalDateTime verifiedAt) {

    public static IdentityVerificationResponse unverified() {
        return new IdentityVerificationResponse(
                IdentityStatus.UNVERIFIED, null, null, false, null, null, null);
    }

    public static IdentityVerificationResponse from(
            IdentityVerification verification, Clock clock, int minimumAge) {
        LocalDate birthDate = verification.getBirthDate();
        boolean adult = verification.getStatus() == IdentityStatus.VERIFIED
                && birthDate != null
                && Period.between(birthDate, LocalDate.now(clock)).getYears() >= minimumAge;
        return new IdentityVerificationResponse(
                verification.getStatus(),
                birthDate,
                verification.getGender(),
                adult,
                verification.getFailureCode(),
                verification.getSubmittedAt(),
                verification.getVerifiedAt());
    }
}

