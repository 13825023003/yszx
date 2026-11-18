package com.yunshu.zhixue.security;

import java.time.Duration;

/**
 * refresh token 服务端快照：刷新时轮换旧令牌，登出/禁用/改密后可批量失效。
 */
public interface RefreshTokenStore {

    void save(TokenDomain domain, Long userId, String refreshJti, Duration ttl);

    boolean isActive(TokenDomain domain, Long userId, String refreshJti);

    void revoke(TokenDomain domain, Long userId, String refreshJti);

    /**
     * 使某用户在某令牌域下的全部 refresh token 失效（禁用账号、修改密码时调用）。
     */
    void revokeAll(TokenDomain domain, Long userId);
}
