package com.yunshu.zhixue.security;

import java.time.Instant;

/**
 * JWT 解析结果。
 *
 * @param userId    用户 id
 * @param domain    令牌域
 * @param account   登录账号（手机号/邮箱/用户名）
 * @param jti       令牌唯一标识（用于黑名单）
 * @param expiresAt 过期时间
 */
public record JwtPrincipal(Long userId,
                           TokenDomain domain,
                           String account,
                           String jti,
                           Instant expiresAt) {
}
