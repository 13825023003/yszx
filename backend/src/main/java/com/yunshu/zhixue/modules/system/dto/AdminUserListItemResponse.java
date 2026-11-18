package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 后台用户列表项（A-M01）。
 */
public record AdminUserListItemResponse(Long id,
                                        String username,
                                        String mobile,
                                        String email,
                                        String avatar,
                                        Long roleId,
                                        String roleName,
                                        String roleCode,
                                        Integer status,
                                        Integer isBuiltin,
                                        LocalDateTime lastLoginAt,
                                        LocalDateTime createdAt) {
}
