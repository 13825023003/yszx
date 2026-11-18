package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 保存角色权限（A-L05）：全量覆盖。
 */
public record RolePermissionSaveRequest(

        @NotNull(message = "权限列表不能为空")
        @Size(max = 200, message = "权限项数量不正确")
        List<@Positive(message = "权限 id 不正确") Long> permissionIds
) {
}
