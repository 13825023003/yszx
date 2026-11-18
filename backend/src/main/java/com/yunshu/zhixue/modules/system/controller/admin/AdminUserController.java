package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.AdminUserBatchRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserBatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetailResponse;
import com.yunshu.zhixue.modules.system.dto.AdminUserListItemResponse;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResponse;
import com.yunshu.zhixue.modules.system.dto.RoleOptionResponse;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台用户管理（A-M01 ~ A-M09）。
 */
@Tag(name = "后台用户管理")
@RestController
@RequestMapping("/api/admin/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表")
    @GetMapping
    @RequirePermission("user:view")
    public ApiResponse<PageResult<AdminUserListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(adminUserService.list(keyword, roleId, status, page, pageSize));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}/detail")
    @RequirePermission("user:view")
    public ApiResponse<AdminUserDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.detail(id));
    }

    @Operation(summary = "可分配角色选项")
    @GetMapping("/role-options")
    @RequirePermission("user:view")
    public ApiResponse<List<RoleOptionResponse>> roleOptions() {
        return ApiResponse.ok(adminUserService.roleOptions());
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @RequirePermission("user:add")
    public ApiResponse<Void> create(@Valid @RequestBody AdminUserCreateRequest request) {
        adminUserService.create(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody AdminUserUpdateRequest request) {
        adminUserService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量启用/禁用")
    @PutMapping("/batch-status")
    @RequirePermission("user:edit")
    public ApiResponse<Void> batchStatus(@Valid @RequestBody AdminUserBatchStatusRequest request) {
        adminUserService.updateStatus(request.ids(), request.status());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @RequirePermission("user:disable")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    @RequirePermission("user:disable")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody AdminUserBatchRequest request) {
        adminUserService.batchDelete(request.ids());
        return ApiResponse.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:edit")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.resetPassword(id));
    }
}
