package com.yunshu.zhixue.security;

import java.time.Instant;

/**
 * access token 黑名单：登出、禁用、改密后使令牌立即失效（D7）。
 */
public interface TokenBlacklistService {

    /**
     * 将令牌写入黑名单，TTL 至令牌自然过期。
     */
    void blacklist(String jti, Instant expiresAt);

    boolean isBlacklisted(String jti);
}
