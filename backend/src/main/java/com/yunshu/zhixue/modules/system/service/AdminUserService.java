package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.BusinessDateUtils;
import com.yunshu.zhixue.common.util.LogicDeleteUtils;
import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetailResponse;
import com.yunshu.zhixue.modules.system.dto.AdminUserListItemResponse;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResponse;
import com.yunshu.zhixue.modules.system.dto.RoleOptionResponse;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.security.RefreshTokenStore;
import com.yunshu.zhixue.security.TokenDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台用户管理（A-M01 ~ A-M09）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int RECENT_LOG_LIMIT = 10;

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${app.default-password}")
    private String defaultPassword;

    public PageResult<AdminUserListItemResponse> list(String keyword, Long roleId, Integer status,
                                                      long page, long pageSize) {
        long current = page < 1 ? AppConstants.DEFAULT_PAGE : page;
        long size = pageSize < 1
                ? AppConstants.DEFAULT_PAGE_SIZE : Math.min(pageSize, AppConstants.MAX_PAGE_SIZE);
        Map<Long, Role> roles = roleMap();

        IPage<AdminUser> result = adminUserMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<AdminUser>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(AdminUser::getUsername, keyword.trim())
                                .or().like(AdminUser::getMobile, keyword.trim()))
                        .eq(roleId != null, AdminUser::getRoleId, roleId)
                        .eq(status != null, AdminUser::getStatus, status)
                        .orderByDesc(AdminUser::getCreatedAt)
                        .orderByDesc(AdminUser::getId));
        List<AdminUserListItemResponse> list = result.getRecords().stream()
                .map(user -> toListItem(user, roles.get(user.getRoleId())))
                .toList();
        return PageResult.of(list, result.getTotal(), current, size);
    }

    public AdminUserDetailResponse detail(Long id) {
        AdminUser user = requireUser(id);
        Role role = roleMapper.selectById(user.getRoleId());
        return new AdminUserDetailResponse(user.getId(), user.getUsername(), user.getMobile(),
                user.getEmail(), user.getAvatar(), user.getRoleId(),
                role == null ? "未知角色" : role.getName(),
                role == null ? "" : role.getCode(),
                user.getStatus(), user.getIsBuiltin(),
                BusinessDateUtils.toBusinessDateTime(user.getLastLoginAt()),
                BusinessDateUtils.toBusinessDateTime(user.getCreatedAt()),
                operationLogService.recentForUser(user.getId(), RECENT_LOG_LIMIT));
    }

    public List<RoleOptionResponse> roleOptions() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .orderByAsc(Role::getId))
                .stream()
                .map(role -> new RoleOptionResponse(role.getId(), role.getName(), role.getCode(),
                        !SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(AdminUserCreateRequest request) {
        String username = request.username().trim();
        String mobile = request.mobile().trim();
        ensureUsernameAvailable(username, null);
        ensureMobileAvailable(mobile, null);

        Role role = requireAssignableRole(request.roleId());
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setMobile(mobile);
        user.setEmail(trimToNull(request.email()));
        user.setAvatar(trimToNull(request.avatar()));
        user.setRoleId(role.getId());
        user.setStatus(request.status() == null ? SystemConstants.STATUS_ENABLED : request.status());
        user.setIsBuiltin(0);
        adminUserMapper.insert(user);

        operationLogService.recordCurrent(SystemConstants.ACTION_CREATE, "后台用户",
                "后台用户「" + username + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminUserUpdateRequest request) {
        AdminUser user = requireUser(id);
        String username = request.username().trim();
        String mobile = request.mobile().trim();
        ensureUsernameAvailable(username, id);
        ensureMobileAvailable(mobile, id);

        boolean builtin = isBuiltin(user);
        Role role;
        if (builtin) {
            if (!request.roleId().equals(user.getRoleId())
                    || !request.status().equals(user.getStatus())) {
                throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                        "内置超级管理员的角色与状态不可修改");
            }
            role = roleMapper.selectById(user.getRoleId());
            if (role == null) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "角色不存在");
            }
        } else {
            role = requireAssignableRole(request.roleId());
        }

        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, id)
                .set(AdminUser::getUsername, username)
                .set(AdminUser::getMobile, mobile)
                .set(AdminUser::getEmail, trimToNull(request.email()))
                .set(AdminUser::getAvatar, trimToNull(request.avatar()))
                .set(AdminUser::getRoleId, role.getId())
                .set(AdminUser::getStatus, request.status()));

        operationLogService.recordCurrent(SystemConstants.ACTION_UPDATE, "后台用户",
                "后台用户「" + username + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(List<Long> ids, Integer status) {
        List<AdminUser> users = listByIds(ids);
        ensureNoBuiltin(users);
        for (AdminUser user : users) {
            adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                    .eq(AdminUser::getId, user.getId())
                    .set(AdminUser::getStatus, status));
            if (status == SystemConstants.STATUS_DISABLED) {
                refreshTokenStore.revokeAll(TokenDomain.ADMIN, user.getId());
            }
        }
        operationLogService.recordCurrent(SystemConstants.ACTION_STATUS, "后台用户",
                "批量" + (status == SystemConstants.STATUS_ENABLED ? "启用" : "禁用")
                        + " " + users.size() + " 个后台用户");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AdminUser user = requireUser(id);
        if (isBuiltin(user)) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED, "内置账号不可删除");
        }
        adminUserMapper.update(null, LogicDeleteUtils.markDeleted(id));
        refreshTokenStore.revokeAll(TokenDomain.ADMIN, id);
        operationLogService.recordCurrent(SystemConstants.ACTION_DELETE, "后台用户",
                "后台用户「" + user.getUsername() + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        List<AdminUser> users = listByIds(ids);
        ensureNoBuiltin(users);
        for (AdminUser user : users) {
            adminUserMapper.update(null, LogicDeleteUtils.markDeleted(user.getId()));
            refreshTokenStore.revokeAll(TokenDomain.ADMIN, user.getId());
        }
        operationLogService.recordCurrent(SystemConstants.ACTION_DELETE, "后台用户",
                "批量删除 " + users.size() + " 个后台用户");
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponse resetPassword(Long id) {
        AdminUser user = requireUser(id);
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, id)
                .set(AdminUser::getPasswordHash, passwordEncoder.encode(defaultPassword)));
        refreshTokenStore.revokeAll(TokenDomain.ADMIN, id);
        operationLogService.recordCurrent(SystemConstants.ACTION_UPDATE, "后台用户",
                "重置后台用户「" + user.getUsername() + "」的登录密码");
        return new ResetPasswordResponse(defaultPassword);
    }

    private AdminUserListItemResponse toListItem(AdminUser user, Role role) {
        return new AdminUserListItemResponse(user.getId(), user.getUsername(), user.getMobile(),
                user.getEmail(), user.getAvatar(), user.getRoleId(),
                role == null ? "未知角色" : role.getName(),
                role == null ? "" : role.getCode(),
                user.getStatus(), user.getIsBuiltin(),
                BusinessDateUtils.toBusinessDateTime(user.getLastLoginAt()),
                BusinessDateUtils.toBusinessDateTime(user.getCreatedAt()));
    }

    private AdminUser requireUser(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<AdminUser> listByIds(List<Long> ids) {
        List<AdminUser> users = adminUserMapper.selectBatchIds(ids);
        if (users.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return users;
    }

    private void ensureNoBuiltin(List<AdminUser> users) {
        boolean hasBuiltin = users.stream().anyMatch(this::isBuiltin);
        if (hasBuiltin) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "内置超级管理员不可执行该操作");
        }
    }

    private boolean isBuiltin(AdminUser user) {
        return Integer.valueOf(SystemConstants.BUILTIN_YES).equals(user.getIsBuiltin());
    }

    private Role requireAssignableRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "角色不存在");
        }
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_ACCOUNT_PROTECTED,
                    "不可创建或切换为超级管理员");
        }
        return role;
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        long count = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, username)
                .ne(excludeId != null, AdminUser::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.NAME_ALREADY_EXISTS, "用户名已存在");
        }
    }

    private void ensureMobileAvailable(String mobile, Long excludeId) {
        long count = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getMobile, mobile)
                .ne(excludeId != null, AdminUser::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.MOBILE_ALREADY_EXISTS);
        }
    }

    private Map<Long, Role> roleMap() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
