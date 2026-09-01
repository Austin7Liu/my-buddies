package com.austin.module.auth.verification;

import java.time.Duration;

public interface VerificationCodeStore {
    CodeIssueResult issue(String phoneKey, String code, Duration expiration, Duration resendCooldown);
    VerificationResult verify(String phoneKey, String submittedCode,
                              int maxFailedAttempts, Duration lockDuration);
    void removeIssuedCode(String phoneKey);
}
