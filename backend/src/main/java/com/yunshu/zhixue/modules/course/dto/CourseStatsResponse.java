package com.yunshu.zhixue.modules.course.dto;

/**
 * 课程统计卡（A-C01）。
 */
public record CourseStatsResponse(long totalCourses,
                                  long totalDurationMinutes,
                                  long associatedSubjectCount) {
}
