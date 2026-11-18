package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionTreeResponse;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RoleListItemResponse;
import com.yunshu.zhixue.modules.system.dto.RolePermissionSaveRequest;
import com.yunshu.zhixue.modules.system.service.AdminRoleService;
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
 * 角色与权限树（A-L01 ~ A-L05）：仅超级管理员可访问。
 */
@Tag(name = "角色权限管理")
@RestController
@RequestMapping("/api/admin/v1")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "角色卡片列表")
    @GetMapping("/roles")
    public ApiResponse<List<RoleListItemResponse>> list() {
        return ApiResponse.ok(adminRoleService.list());
    }

    @Operation(summary = "新增角色")
    @PostMapping("/roles")
    public ApiResponse<Void> create(@Valid @RequestBody RoleCreateRequest request) {
        adminRoleService.create(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminRoleService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "权限树")
    @GetMapping("/permissions/tree")
    public ApiResponse<PermissionTreeResponse> tree(
            @RequestParam(required = false) Long roleId) {
        return ApiResponse.ok(adminRoleService.tree(roleId));
    }

    @Operation(summary = "保存角色权限")
    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<Void> savePermissions(@PathVariable Long id,
                                             @Valid @RequestBody RolePermissionSaveRequest request) {
        adminRoleService.savePermissions(id, request.permissionIds());
        return ApiResponse.ok();
    }
}
