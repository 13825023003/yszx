package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.security.PermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 管理端权限码提供者实现：登录态变化或角色权限保存后均按库实时读取。
 */
@Primary
@Component
@RequiredArgsConstructor
public class SystemPermissionProvider implements PermissionProvider {

    private final AdminPermissionService adminPermissionService;

    @Override
    public Set<String> getPermissions(Long adminUserId) {
        return adminPermissionService.getPermissionCodes(adminUserId);
    }
}
