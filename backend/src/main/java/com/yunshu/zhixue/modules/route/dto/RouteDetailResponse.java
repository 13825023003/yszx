package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * 路线详情 / 预览（A-R06）。
 */
public record RouteDetailResponse(Long id,
                                  String name,
                                  Long subjectId,
                                  String subjectName,
                                  Integer status,
                                  String icon,
                                  String targetEducation,
                                  String description,
                                  long courseCount,
                                  long totalDurationMinutes,
                                  List<RouteCourseItemResponse> courses) {
}
