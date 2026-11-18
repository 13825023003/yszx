package com.yunshu.zhixue.security;

import java.util.Set;

/**
 * 管理员权限码提供者：由 system 模块在实施阶段实现（按角色查询权限集合，可加缓存）。
 *
 * <p>返回 {@code "*"} 表示超级管理员拥有全部权限。</p>
 */
@FunctionalInterface
public interface PermissionProvider {

    Set<String> getPermissions(Long adminUserId);
}
