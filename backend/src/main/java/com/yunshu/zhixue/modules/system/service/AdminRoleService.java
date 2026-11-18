package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.util.LogicDeleteUtils;
import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.dto.PermissionTreeResponse;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RoleListItemResponse;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Permission;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色与权限树（A-L01 ~ A-L05）：全部操作仅超级管理员可执行（D4-5）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private static final int TYPE_GROUP = 1;
    private static final int TYPE_POINT = 2;

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminPermissionService adminPermissionService;
    private final OperationLogService operationLogService;

    public List<RoleListItemResponse> list() {
        adminPermissionService.assertSuperAdmin();
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                .orderByAsc(Role::getId));
        List<Permission> permissions = permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                .orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId));
        Map<Long, Permission> groupById = permissions.stream()
                .filter(item -> item.getType() == TYPE_GROUP)
                .collect(Collectors.toMap(Permission::getId, item -> item));
        Map<Long, Set<Long>> rolePermissionIds = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery())
                .stream()
                .collect(Collectors.groupingBy(RolePermission::getRoleId,
                        Collectors.mapping(RolePermission::getPermissionId,
                                Collectors.toCollection(HashSet::new))));
        Map<Long, Long> userCounts = adminUserMapper.selectList(
                        Wrappers.<AdminUser>lambdaQuery().select(AdminUser::getRoleId))
                .stream()
                .collect(Collectors.groupingBy(AdminUser::getRoleId, Collectors.counting()));

        return roles.stream().map(role -> {
            List<String> labels = permissionLabels(role,
                    rolePermissionIds.getOrDefault(role.getId(), Set.of()),
                    permissions, groupById);
            return new RoleListItemResponse(role.getId(), role.getName(), role.getCode(),
                    role.getDescription(), userCounts.getOrDefault(role.getId(), 0L), labels);
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(RoleCreateRequest request) {
        adminPermissionService.assertSuperAdmin();
        String code = request.code().trim();
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(code)) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "超级管理员角色为系统内置");
        }
        long exists = roleMapper.selectCount(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, code));
        if (exists > 0) {
            throw new BizException(ErrorCode.NAME_ALREADY_EXISTS, "角色编码已存在");
        }
        Role role = new Role();
        role.setName(request.name().trim());
        role.setCode(code);
        role.setDescription(StringUtils.hasText(request.description())
                ? request.description().trim() : null);
        roleMapper.insert(role);
        operationLogService.recordCurrent(SystemConstants.ACTION_CREATE, "角色",
                "角色「" + role.getName() + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        adminPermissionService.assertSuperAdmin();
        Role role = requireRole(id);
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "超级管理员角色不可删除");
        }
        long userCount = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getRoleId, id));
        if (userCount > 0) {
            throw new BizException(ErrorCode.ROLE_IN_USE);
        }
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, id));
        roleMapper.update(null, LogicDeleteUtils.markDeleted(id));
        operationLogService.recordCurrent(SystemConstants.ACTION_DELETE, "角色",
                "角色「" + role.getName() + "」");
    }

    public PermissionTreeResponse tree(Long roleId) {
        adminPermissionService.assertSuperAdmin();
        return adminPermissionService.getTree(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePermissions(Long roleId, List<Long> permissionIds) {
        adminPermissionService.assertSuperAdmin();
        Role role = requireRole(roleId);
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "超级管理员默认拥有全部权限，不可修改");
        }
        List<Long> ids = permissionIds.stream().distinct().toList();
        if (!ids.isEmpty()) {
            List<Permission> points = permissionMapper.selectBatchIds(ids);
            boolean invalid = points.size() != ids.size()
                    || points.stream().anyMatch(item -> item.getType() != TYPE_POINT);
            if (invalid) {
                throw new BizException(ErrorCode.PERMISSION_SAVE_FAILED, "存在无效的权限项");
            }
        }
        try {
            rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                    .eq(RolePermission::getRoleId, roleId));
            for (Long permissionId : ids) {
                RolePermission relation = new RolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                rolePermissionMapper.insert(relation);
            }
        } catch (Exception ex) {
            log.error("保存角色权限失败: roleId={}", roleId, ex);
            throw new BizException(ErrorCode.PERMISSION_SAVE_FAILED);
        }
        operationLogService.recordCurrent(SystemConstants.ACTION_UPDATE, "角色",
                "角色「" + role.getName() + "」权限（" + ids.size() + " 项）");
    }

    private List<String> permissionLabels(Role role, Set<Long> checkedIds,
                                          List<Permission> permissions,
                                          Map<Long, Permission> groupById) {
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            return List.of("全部权限");
        }
        if (checkedIds.isEmpty()) {
            return List.of("暂无权限");
        }
        List<String> labels = new ArrayList<>();
        for (Permission permission : permissions) {
            if (permission.getType() != TYPE_POINT || !checkedIds.contains(permission.getId())) {
                continue;
            }
            Permission group = groupById.get(permission.getParentId());
            labels.add(group == null
                    ? permission.getName()
                    : group.getName() + " · " + permission.getName());
        }
        return labels;
    }

    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
