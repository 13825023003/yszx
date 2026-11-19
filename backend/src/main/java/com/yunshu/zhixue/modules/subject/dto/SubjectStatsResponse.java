package com.yunshu.zhixue.modules.subject.dto;

/**
 * 学科统计卡（A-S01）。
 */
public record SubjectStatsResponse(long totalSubjects,
                                   long subjectsWithStages,
                                   long totalStages,
                                   long totalQuestions) {
}
