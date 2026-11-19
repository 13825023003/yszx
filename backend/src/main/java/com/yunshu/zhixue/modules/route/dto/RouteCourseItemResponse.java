package com.yunshu.zhixue.modules.route.dto;

import com.yunshu.zhixue.modules.subject.dto.SubjectBrief;

import java.util.List;

/**
 * 路线中的课程项。
 */
public record RouteCourseItemResponse(Long id,
                                      String name,
                                      String coverUrl,
                                      Integer durationMinutes,
                                      Integer source,
                                      String link,
                                      String bvid,
                                      SubjectBrief primarySubject,
                                      List<SubjectBrief> subjects,
                                      Integer sortOrder,
                                      boolean localSubject) {
}
