package com.yunshu.zhixue.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 基于 Redis 的 refresh token 快照。
 *
 * <p>单令牌：{@code token:refresh:{domain}:{userId}:{jti}}；
 * 用户级集合：{@code token:refresh:user:{domain}:{userId}}，用于批量失效。</p>
 */
@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "token:refresh:";
    private static final String USER_KEY_PREFIX = "token:refresh:user:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(TokenDomain domain, Long userId, String refreshJti, Duration ttl) {
        String userKey = userKey(domain, userId);
        stringRedisTemplate.opsForValue().set(tokenKey(domain, userId, refreshJti), "1", ttl);
        stringRedisTemplate.opsForSet().add(userKey, refreshJti);
        stringRedisTemplate.expire(userKey, ttl);
    }

    @Override
    public boolean isActive(TokenDomain domain, Long userId, String refreshJti) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(tokenKey(domain, userId, refreshJti)));
    }

    @Override
    public void revoke(TokenDomain domain, Long userId, String refreshJti) {
        stringRedisTemplate.delete(tokenKey(domain, userId, refreshJti));
        stringRedisTemplate.opsForSet().remove(userKey(domain, userId), refreshJti);
    }

    @Override
    public void revokeAll(TokenDomain domain, Long userId) {
        String userKey = userKey(domain, userId);
        Set<String> jtis = stringRedisTemplate.opsForSet().members(userKey);
        if (jtis != null) {
            for (String jti : jtis) {
                stringRedisTemplate.delete(tokenKey(domain, userId, jti));
            }
        }
        stringRedisTemplate.delete(userKey);
    }

    private String tokenKey(TokenDomain domain, Long userId, String jti) {
        return TOKEN_KEY_PREFIX + domain.getCode() + ":" + userId + ":" + jti;
    }

    private String userKey(TokenDomain domain, Long userId) {
        return USER_KEY_PREFIX + domain.getCode() + ":" + userId;
    }
}
