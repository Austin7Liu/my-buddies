package com.austin.module.auth.verification;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisVerificationCodeStore implements VerificationCodeStore {

    private static final String PREFIX = "auth:sms:";
    private static final DefaultRedisScript<Long> ISSUE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[3]) == 1 then return -1 end
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('SET', KEYS[2], '1', 'PX', ARGV[3])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[3]) == 1 then return -2 end
            local expected = redis.call('GET', KEYS[1])
            if not expected then return 0 end
            if expected == ARGV[1] then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                return 1
            end
            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then redis.call('PEXPIRE', KEYS[2], ARGV[3]) end
            if attempts >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[3], '1', 'PX', ARGV[3])
                redis.call('DEL', KEYS[1])
                return -2
            end
            return -1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public CodeIssueResult issue(String phoneKey, String code, Duration expiration, Duration resendCooldown) {
        Long result = redisTemplate.execute(
                ISSUE_SCRIPT,
                List.of(codeKey(phoneKey), cooldownKey(phoneKey), lockKey(phoneKey)),
                code,
                String.valueOf(expiration.toMillis()),
                String.valueOf(resendCooldown.toMillis()));
        if (result == null) {
            throw new IllegalStateException("Redis 未返回验证码写入结果");
        }
        return switch (result.intValue()) {
            case 1 -> CodeIssueResult.ISSUED;
            case 0 -> CodeIssueResult.TOO_FREQUENT;
            case -1 -> CodeIssueResult.LOCKED;
            default -> throw new IllegalStateException("未知验证码写入结果: " + result);
        };
    }

    @Override
    public VerificationResult verify(
            String phoneKey, String submittedCode, int maxFailedAttempts, Duration lockDuration) {
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(codeKey(phoneKey), attemptsKey(phoneKey), lockKey(phoneKey)),
                submittedCode,
                String.valueOf(maxFailedAttempts),
                String.valueOf(lockDuration.toMillis()));
        if (result == null) {
            throw new IllegalStateException("Redis 未返回验证码校验结果");
        }
        return switch (result.intValue()) {
            case 1 -> VerificationResult.SUCCESS;
            case 0 -> VerificationResult.EXPIRED;
            case -1 -> VerificationResult.INVALID;
            case -2 -> VerificationResult.LOCKED;
            default -> throw new IllegalStateException("未知验证码校验结果: " + result);
        };
    }

    @Override
    public void removeIssuedCode(String phoneKey) {
        redisTemplate.delete(List.of(codeKey(phoneKey), cooldownKey(phoneKey)));
    }

    private String codeKey(String phoneKey) { return PREFIX + "code:" + phoneKey; }
    private String cooldownKey(String phoneKey) { return PREFIX + "cooldown:" + phoneKey; }
    private String attemptsKey(String phoneKey) { return PREFIX + "attempts:" + phoneKey; }
    private String lockKey(String phoneKey) { return PREFIX + "lock:" + phoneKey; }
}
