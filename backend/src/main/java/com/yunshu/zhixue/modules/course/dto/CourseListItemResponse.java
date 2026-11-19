package com.yunshu.zhixue.modules.course.dto;

import com.yunshu.zhixue.modules.subject.dto.SubjectBrief;

import java.util.List;

/**
 * 课程列表项（A-C02）：主学科 + 额外学科。
 */
public record CourseListItemResponse(Long id,
                                     String name,
                                     String coverUrl,
                                     Integer durationMinutes,
                                     Integer source,
                                     String link,
                                     String bvid,
                                     SubjectBrief primarySubject,
                                     List<SubjectBrief> subjects) {
}
