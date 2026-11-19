package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * Excel 导入预览行。
 *
 * @param status valid / warning / invalid
 */
public record CourseImportRowResponse(int rowNumber,
                                      String name,
                                      String subjectText,
                                      String sourceText,
                                      Integer durationMinutes,
                                      String bvid,
                                      String coverUrl,
                                      String link,
                                      String status,
                                      List<String> messages) {
}
