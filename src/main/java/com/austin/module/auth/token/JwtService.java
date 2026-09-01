package com.austin.module.auth.token;

import com.austin.module.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String create(long accountId, TokenType tokenType, String sessionId) {
        Instant issuedAt = clock.instant();
        Duration lifetime = tokenType == TokenType.ACCESS
                ? properties.accessTokenExpiration() : properties.refreshTokenExpiration();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(accountId))
                .id(UUID.randomUUID().toString())
                .claim("token_type", tokenType.name())
                .claim("session_id", sessionId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(lifetime)))
                .signWith(signingKey)
                .compact();
    }

    public TokenClaims parse(String token, TokenType expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        TokenType actualType = TokenType.valueOf(claims.get("token_type", String.class));
        if (actualType != expectedType) {
            throw new IllegalArgumentException("令牌类型不正确");
        }
        String sessionId = claims.get("session_id", String.class);
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("令牌缺少会话标识");
        }
        return new TokenClaims(
                Long.parseLong(claims.getSubject()),
                claims.getId(),
                sessionId,
                actualType,
                claims.getExpiration().toInstant());
    }
}
