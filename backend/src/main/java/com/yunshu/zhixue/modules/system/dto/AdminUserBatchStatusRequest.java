package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 批量启用/禁用后台用户（A-M04）。
 */
public record AdminUserBatchStatusRequest(

        @NotEmpty(message = "请选择用户")
        List<@Positive(message = "用户 id 不正确") Long> ids,

        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态不正确")
        @Max(value = 1, message = "状态不正确")
        Integer status
) {
}
