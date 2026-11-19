package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 确认导入（A-C07）：携带预览 token。
 */
public record CourseImportConfirmRequest(

        @NotBlank(message = "导入预览 token 不能为空")
        String token
) {
}
