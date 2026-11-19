package com.yunshu.zhixue.modules.subject.dto;

/**
 * 学科卡片中的阶段行（A-S02）。
 */
public record StageItemResponse(Long id,
                                String name,
                                String icon,
                                Integer sortOrder,
                                Integer status,
                                long questionCount) {
}
