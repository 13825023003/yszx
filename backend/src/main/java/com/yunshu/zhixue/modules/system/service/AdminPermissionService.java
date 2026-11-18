package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.dto.PermissionGroupResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionItemResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionTreeResponse;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Permission;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.mapper.RolePermissionMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限查询与超管判定：权限切面与认证信息接口共用。
 *
 * <p>按角色实时查询，权限保存后立即生效；管理端账号数量有限，暂不加缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private static final int TYPE_GROUP = 1;
    private static final int TYPE_POINT = 2;

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public Set<String> getPermissionCodes(Long adminUserId) {
        AdminUser user = adminUserMapper.selectById(adminUserId);
        if (user == null) {
            return Set.of();
        }
        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            return Set.of();
        }
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            return Set.of(SystemConstants.WILDCARD_PERMISSION);
        }
        return new HashSet<>(permissionMapper.selectCodesByRoleId(role.getId()));
    }

    public boolean isSuperAdmin(Long adminUserId) {
        AdminUser user = adminUserMapper.selectById(adminUserId);
        if (user == null) {
            return false;
        }
        Role role = roleMapper.selectById(user.getRoleId());
        return role != null
                && SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode());
    }

    public void assertSuperAdmin() {
        if (!isSuperAdmin(SecurityUtils.requireUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅超级管理员可访问");
        }
    }

    public PermissionTreeResponse getTree(Long roleId) {
        List<Permission> all = permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                .orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId));
        Set<Long> checked;
        if (roleId != null) {
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
            }
            checked = rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                            .eq(RolePermission::getRoleId, roleId))
                    .stream()
                    .map(RolePermission::getPermissionId)
                    .collect(Collectors.toSet());
        } else {
            checked = Set.of();
        }

        List<Permission> groups = all.stream()
                .filter(item -> item.getType() == TYPE_GROUP)
                .toList();
        List<PermissionGroupResponse> result = new ArrayList<>(groups.size());
        long total = 0;
        for (Permission group : groups) {
            List<PermissionItemResponse> children = all.stream()
                    .filter(item -> item.getType() == TYPE_POINT)
                    .filter(item -> group.getId().equals(item.getParentId()))
                    .map(item -> new PermissionItemResponse(item.getId(), item.getName(),
                            item.getCode(), checked.contains(item.getId())))
                    .toList();
            total += children.size();
            result.add(new PermissionGroupResponse(group.getId(), group.getName(),
                    group.getCode(), children));
        }
        return new PermissionTreeResponse(result, total);
    }
}
