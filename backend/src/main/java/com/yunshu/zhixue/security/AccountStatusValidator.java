package com.yunshu.zhixue.security;

/**
 * 已签发 access token 的账号状态校验（禁用/删除后立即失效）。
 *
 * <p>由各业务模块按令牌域实现；未提供实现时视为有效。仅对 ADMIN 域启用。</p>
 */
@FunctionalInterface
public interface AccountStatusValidator {

    boolean isActive(TokenDomain domain, Long userId);
}
