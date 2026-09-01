package com.austin.module.auth.token;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Component
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private static final String REFRESH_PREFIX = "auth:jwt:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:jwt:blacklist:";
    private static final String REVOKED_SESSION_PREFIX = "auth:jwt:revoked-session:";
    private static final DefaultRedisScript<String> CONSUME_REFRESH_SCRIPT =
            new DefaultRedisScript<>("""
                    local value = redis.call('GET', KEYS[1])
                    if value then redis.call('DEL', KEYS[1]) end
                    return value
                    """, String.class);
    private final StringRedisTemplate redisTemplate;

    @Override
    public void storeRefreshToken(String tokenId, long accountId, Duration ttl) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + tokenId, Long.toString(accountId), ttl);
    }

    @Override
    public boolean consumeRefreshToken(String tokenId, long accountId) {
        String storedAccountId = redisTemplate.execute(
                CONSUME_REFRESH_SCRIPT, List.of(REFRESH_PREFIX + tokenId));
        return Long.toString(accountId).equals(storedAccountId);
    }

    @Override
    public void revokeRefreshToken(String tokenId) {
        redisTemplate.delete(REFRESH_PREFIX + tokenId);
    }

    @Override
    public void blacklistAccessToken(String tokenId, Duration ttl) {
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + tokenId, "1", ttl);
        }
    }

    @Override
    public boolean isAccessTokenBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId));
    }

    @Override
    public void revokeSession(String sessionId, Duration ttl) {
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(REVOKED_SESSION_PREFIX + sessionId, "1", ttl);
        }
    }

    @Override
    public boolean isSessionRevoked(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_SESSION_PREFIX + sessionId));
    }
}
