package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 权限树（9 组 34 项，A-L04）。
 */
public record PermissionTreeResponse(List<PermissionGroupResponse> groups, long total) {
}
