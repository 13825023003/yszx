package com.yunshu.zhixue.modules.subject.dto;

import java.util.List;

/**
 * 学科卡片（A-S02）：同时携带删除提示所需的关联数量。
 */
public record SubjectListItemResponse(Long id,
                                      String name,
                                      String icon,
                                      String description,
                                      String themeColor,
                                      Integer sortOrder,
                                      long courseCount,
                                      long routeCount,
                                      long questionCount,
                                      long interviewQuestionCount,
                                      long stageCount,
                                      long enabledStageCount,
                                      List<StageItemResponse> stages) {
}
