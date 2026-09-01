package com.austin.module.auth.service;

import com.austin.common.exception.ForbiddenException;
import com.austin.common.exception.TooManyRequestsException;
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
import com.austin.module.auth.token.TokenPair;
import com.austin.module.auth.token.TokenStore;
import com.austin.module.auth.token.TokenType;
import com.austin.module.auth.verification.CodeIssueResult;
import com.austin.module.auth.verification.VerificationCodeStore;
import com.austin.module.auth.verification.VerificationResult;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final VerificationCodeStore verificationCodeStore;
    private final SmsSender smsSender;
    private final PhoneKeyHasher phoneKeyHasher;
    private final SmsCodeProperties smsCodeProperties;
    private final UserAccountService userAccountService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;
    private final Clock clock;

    public long sendVerificationCode(String phone) {
        String code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        String phoneKey = phoneKeyHasher.hash(phone);
        CodeIssueResult result = verificationCodeStore.issue(
                phoneKey, code, smsCodeProperties.expiration(), smsCodeProperties.resendCooldown());
        if (result == CodeIssueResult.TOO_FREQUENT) {
            throw new TooManyRequestsException("验证码发送过于频繁，请稍后重试");
        }
        if (result == CodeIssueResult.LOCKED) {
            throw new TooManyRequestsException("验证码尝试次数过多，请稍后重试");
        }
        try {
            smsSender.sendVerificationCode(phone, code);
        } catch (RuntimeException exception) {
            verificationCodeStore.removeIssuedCode(phoneKey);
            throw exception;
        }
        return smsCodeProperties.expiration().toSeconds();
    }

    public AuthResult loginOrRegister(String phone, String submittedCode) {
        VerificationResult result = verificationCodeStore.verify(
                phoneKeyHasher.hash(phone), submittedCode,
                smsCodeProperties.maxFailedAttempts(), smsCodeProperties.lockDuration());
        switch (result) {
            case INVALID -> throw new UnauthorizedException("验证码错误");
            case EXPIRED -> throw new UnauthorizedException("验证码不存在或已过期");
            case LOCKED -> throw new TooManyRequestsException("验证码尝试次数过多，请稍后重试");
            case SUCCESS -> { }
        }
        UserAccount account = userAccountService.findOrCreateByPhone(phone);
        ensureLoginAllowed(account);
        return new AuthResult(account, issueTokenPair(account.getId()));
    }

    public TokenPair refresh(String refreshToken) {
        TokenClaims claims;
        try {
            claims = jwtService.parse(refreshToken, TokenType.REFRESH);
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("刷新令牌无效或已过期", exception);
        }
        if (!tokenStore.consumeRefreshToken(claims.tokenId(), claims.accountId())) {
            throw new UnauthorizedException("刷新令牌已失效");
        }
        if (tokenStore.isSessionRevoked(claims.sessionId())) {
            throw new UnauthorizedException("登录会话已退出");
        }
        ensureLoginAllowed(userAccountService.getById(claims.accountId()));
        return issueTokenPair(claims.accountId(), claims.sessionId());
    }

    public void logout(long accountId, String accessToken, String refreshToken) {
        try {
            TokenClaims accessClaims = jwtService.parse(accessToken, TokenType.ACCESS);
            TokenClaims refreshClaims = jwtService.parse(refreshToken, TokenType.REFRESH);
            if (accessClaims.accountId() != accountId || refreshClaims.accountId() != accountId) {
                throw new UnauthorizedException("令牌与当前账户不匹配");
            }
            if (!accessClaims.sessionId().equals(refreshClaims.sessionId())) {
                throw new UnauthorizedException("访问令牌与刷新令牌不属于同一登录会话");
            }
            tokenStore.revokeRefreshToken(refreshClaims.tokenId());
            Duration remaining = Duration.between(clock.instant(), accessClaims.expiresAt());
            tokenStore.blacklistAccessToken(accessClaims.tokenId(), remaining);
            Duration sessionRemaining = Duration.between(clock.instant(), refreshClaims.expiresAt());
            tokenStore.revokeSession(accessClaims.sessionId(), sessionRemaining);
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("退出登录令牌无效", exception);
        }
    }

    private TokenPair issueTokenPair(long accountId) {
        return issueTokenPair(accountId, java.util.UUID.randomUUID().toString());
    }

    private TokenPair issueTokenPair(long accountId, String sessionId) {
        String accessToken = jwtService.create(accountId, TokenType.ACCESS, sessionId);
        String refreshToken = jwtService.create(accountId, TokenType.REFRESH, sessionId);
        TokenClaims refreshClaims = jwtService.parse(refreshToken, TokenType.REFRESH);
        tokenStore.storeRefreshToken(
                refreshClaims.tokenId(), accountId, jwtProperties.refreshTokenExpiration());
        return new TokenPair(accessToken, refreshToken, jwtProperties.accessTokenExpiration().toSeconds());
    }

    private void ensureLoginAllowed(UserAccount account) {
        if (account.getAccountStatus() == AccountStatus.BANNED) {
            throw new ForbiddenException("账户已被永久封禁");
        }
        if (account.getAccountStatus() == AccountStatus.CANCELLED) {
            throw new ForbiddenException("账户已注销");
        }
    }

    public record AuthResult(UserAccount account, TokenPair tokens) { }
}
