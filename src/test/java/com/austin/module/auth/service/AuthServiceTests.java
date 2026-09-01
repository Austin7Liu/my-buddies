package com.austin.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.common.exception.UnauthorizedException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.auth.config.JwtProperties;
import com.austin.module.auth.config.SmsCodeProperties;
import com.austin.module.auth.sms.SmsSender;
import com.austin.module.auth.support.PhoneKeyHasher;
import com.austin.module.auth.token.JwtService;
import com.austin.module.auth.token.TokenClaims;
import com.austin.module.auth.token.TokenStore;
import com.austin.module.auth.token.TokenType;
import com.austin.module.auth.verification.CodeIssueResult;
import com.austin.module.auth.verification.VerificationCodeStore;
import com.austin.module.auth.verification.VerificationResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final String PHONE = "13800138000";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T06:00:00Z"), ZoneOffset.UTC);

    @Mock private VerificationCodeStore verificationCodeStore;
    @Mock private SmsSender smsSender;
    @Mock private PhoneKeyHasher phoneKeyHasher;
    @Mock private UserAccountService userAccountService;
    @Mock private TokenStore tokenStore;
    private AuthService authService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SmsCodeProperties smsProperties = new SmsCodeProperties(
                Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(5));
        JwtProperties jwtProperties = new JwtProperties(
                "my-buddies", "my-buddies-test-secret-at-least-32-bytes",
                Duration.ofMinutes(30), Duration.ofDays(7));
        jwtService = new JwtService(jwtProperties, CLOCK);
        authService = new AuthService(
                verificationCodeStore, smsSender, phoneKeyHasher, smsProperties,
                userAccountService, jwtService, jwtProperties, tokenStore, CLOCK);
        when(phoneKeyHasher.hash(PHONE)).thenReturn("phone-hash");
    }

    @Test
    void generatesAndSendsSixDigitCode() {
        when(verificationCodeStore.issue(eq("phone-hash"), any(),
                eq(Duration.ofMinutes(5)), eq(Duration.ofSeconds(60))))
                .thenReturn(CodeIssueResult.ISSUED);

        assertThat(authService.sendVerificationCode(PHONE)).isEqualTo(300);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsSender).sendVerificationCode(eq(PHONE), codeCaptor.capture());
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void rejectsWrongCodeWithoutCreatingAccount() {
        when(verificationCodeStore.verify("phone-hash", "000000", 5, Duration.ofMinutes(5)))
                .thenReturn(VerificationResult.INVALID);

        assertThatThrownBy(() -> authService.loginOrRegister(PHONE, "000000"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("验证码错误");
        verify(userAccountService, never()).findOrCreateByPhone(any());
    }

    @Test
    void logsInAndIssuesTypedJwtPairAfterSuccessfulVerification() {
        when(verificationCodeStore.verify("phone-hash", "123456", 5, Duration.ofMinutes(5)))
                .thenReturn(VerificationResult.SUCCESS);
        when(userAccountService.findOrCreateByPhone(PHONE)).thenReturn(UserAccount.builder()
                .id(42L).phone(PHONE).accountStatus(AccountStatus.ACTIVE).build());

        AuthService.AuthResult result = authService.loginOrRegister(PHONE, "123456");

        assertThat(result.account().getId()).isEqualTo(42L);
        TokenClaims access = jwtService.parse(result.tokens().accessToken(), TokenType.ACCESS);
        TokenClaims refresh = jwtService.parse(result.tokens().refreshToken(), TokenType.REFRESH);
        assertThat(access.accountId()).isEqualTo(42L);
        assertThat(refresh.accountId()).isEqualTo(42L);
        assertThat(access.sessionId()).isEqualTo(refresh.sessionId());
        verify(tokenStore).storeRefreshToken(refresh.tokenId(), 42L, Duration.ofDays(7));
    }
}
