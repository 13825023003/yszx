package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 权限树分组（A-L04）。
 */
public record PermissionGroupResponse(Long id,
                                      String name,
                                      String code,
                                      List<PermissionItemResponse> permissions) {
}
