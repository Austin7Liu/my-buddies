package com.austin.module.auth.token;

import java.time.Duration;

public interface TokenStore {
    void storeRefreshToken(String tokenId, long accountId, Duration ttl);
    boolean consumeRefreshToken(String tokenId, long accountId);
    void revokeRefreshToken(String tokenId);
    void blacklistAccessToken(String tokenId, Duration ttl);
    boolean isAccessTokenBlacklisted(String tokenId);
    void revokeSession(String sessionId, Duration ttl);
    boolean isSessionRevoked(String sessionId);
}
