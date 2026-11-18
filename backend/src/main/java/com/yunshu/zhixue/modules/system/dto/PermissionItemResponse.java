package com.yunshu.zhixue.modules.system.dto;

/**
 * 权限树操作点（A-L04）。
 */
public record PermissionItemResponse(Long id,
                                     String name,
                                     String code,
                                     boolean checked) {
}
