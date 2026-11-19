package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * 已通过校验、待写入的导入课程数据（内部使用）。
 */
public record CourseImportData(String name,
                               Integer durationMinutes,
                               Long primarySubjectId,
                               List<Long> extraSubjectIds,
                               Integer source,
                               String link,
                               String coverUrl,
                               String bvid) {
}
