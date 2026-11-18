package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户详情（A-M08）：资料 + 最近操作。
 */
public record AdminUserDetailResponse(Long id,
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
                                      LocalDateTime createdAt,
                                      List<OperationLogResponse> recentLogs) {
}
