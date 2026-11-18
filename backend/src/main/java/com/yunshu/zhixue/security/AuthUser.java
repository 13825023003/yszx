package com.yunshu.zhixue.security;

/**
 * 当前请求的已认证用户（放入 SecurityContext 的 principal）。
 */
public record AuthUser(Long id, String account, TokenDomain domain) {
}
