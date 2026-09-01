package com.austin.module.identity.provider;

import com.austin.module.identity.domain.Gender;
import java.time.LocalDate;

public record IdentityProviderResult(
        boolean verified,
        String subjectFingerprint,
        LocalDate birthDate,
        Gender gender,
        String provider,
        String providerReference,
        String failureCode) {

    public static IdentityProviderResult failed(String provider, String failureCode) {
        return new IdentityProviderResult(false, null, null, null, provider, null, failureCode);
    }
}

