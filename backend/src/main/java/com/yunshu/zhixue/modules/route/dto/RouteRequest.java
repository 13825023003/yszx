package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 新建 / 编辑路线基本信息（A-R02 / A-R03）。
 */
public record RouteRequest(

        @NotBlank(message = "请输入路线名称")
        @Size(max = 100, message = "路线名称不能超过 100 个字符")
        String name,

        @NotNull(message = "请选择关联学科")
        @Positive(message = "关联学科不正确")
        Long subjectId,

        @Size(max = 100, message = "适合学历不能超过 100 个字符")
        String targetEducation,

        @Size(max = 50, message = "路线图标长度不正确")
        String icon,

        @Size(max = 1000, message = "路线描述不能超过 1000 个字符")
        String description,

        @Min(value = 0, message = "路线状态不正确")
        @Max(value = 1, message = "路线状态不正确")
        Integer status
) {
}
