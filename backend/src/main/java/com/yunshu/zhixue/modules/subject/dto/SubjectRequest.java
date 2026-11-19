package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 编辑学科（A-S03 / A-S04）。
 */
public record SubjectRequest(

        @NotBlank(message = "请输入学科名称")
        @Size(max = 50, message = "学科名称不能超过 50 个字符")
        String name,

        @NotBlank(message = "请输入学科图标")
        @Size(max = 50, message = "学科图标长度不正确")
        String icon,

        @NotBlank(message = "请输入学科描述")
        @Size(max = 255, message = "学科描述不能超过 255 个字符")
        String description,

        @NotNull(message = "请输入排序号")
        @Min(value = 1, message = "排序号需大于 0")
        @Max(value = 999999, message = "排序号过大")
        Integer sortOrder,

        @NotBlank(message = "请选择主题颜色")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "主题颜色需为十六进制颜色值")
        String themeColor
) {
}
