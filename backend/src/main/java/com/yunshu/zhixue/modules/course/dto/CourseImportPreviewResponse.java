package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * Excel 导入预览（A-C06）。
 */
public record CourseImportPreviewResponse(String token,
                                          String filename,
                                          int totalRows,
                                          int validCount,
                                          int warningCount,
                                          int invalidCount,
                                          List<CourseImportRowResponse> rows) {
}
