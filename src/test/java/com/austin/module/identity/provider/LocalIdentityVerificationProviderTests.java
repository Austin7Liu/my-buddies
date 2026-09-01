package com.austin.module.identity.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.austin.module.identity.config.IdentityProperties;
import com.austin.module.identity.domain.Gender;
import org.junit.jupiter.api.Test;

class LocalIdentityVerificationProviderTests {

    private final LocalIdentityVerificationProvider provider = new LocalIdentityVerificationProvider(
            new IdentityProperties("test-identity-fingerprint-secret-32-characters", 18));

    @Test
    void derivesOnlyNecessaryAttributesFromValidIdentityNumber() {
        IdentityProviderResult result = provider.verify(
                new IdentityVerificationCommand("测试用户", "11010519491231002X"));

        assertThat(result.verified()).isTrue();
        assertThat(result.birthDate()).hasToString("1949-12-31");
        assertThat(result.gender()).isEqualTo(Gender.FEMALE);
        assertThat(result.subjectFingerprint()).hasSize(64).doesNotContain("11010519491231002X");
    }

    @Test
    void rejectsIdentityNumberWithInvalidChecksum() {
        IdentityProviderResult result = provider.verify(
                new IdentityVerificationCommand("测试用户", "110105194912310020"));

        assertThat(result.verified()).isFalse();
        assertThat(result.failureCode()).isEqualTo("INVALID_IDENTITY_NUMBER");
        assertThat(result.subjectFingerprint()).isNull();
    }
}

