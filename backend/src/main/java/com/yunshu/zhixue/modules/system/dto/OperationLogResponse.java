package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 操作日志列表/详情（A-G01、A-G02）。
 */
public record OperationLogResponse(Long id,
                                   Long userId,
                                   String username,
                                   String action,
                                   String targetType,
                                   String targetName,
                                   String ip,
                                   LocalDateTime createdAt) {
}
