package com.austin.module.auth.token;

import java.time.Instant;

public record TokenClaims(long accountId, String tokenId, String sessionId,
                          TokenType tokenType, Instant expiresAt) {
}
