package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 阶段启用 / 禁用（A-S10）。
 */
public record StageStatusRequest(

        @NotNull(message = "请选择阶段状态")
        @Min(value = 0, message = "阶段状态不正确")
        @Max(value = 1, message = "阶段状态不正确")
        Integer status
) {
}
