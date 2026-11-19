package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 发布 / 下架路线（A-R04）。
 */
public record RouteStatusRequest(

        @NotNull(message = "请选择路线状态")
        @Min(value = 0, message = "路线状态不正确")
        @Max(value = 1, message = "路线状态不正确")
        Integer status
) {
}
