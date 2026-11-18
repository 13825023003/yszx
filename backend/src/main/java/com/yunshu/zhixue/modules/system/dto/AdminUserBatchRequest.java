package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 批量删除后台用户（A-M06）。
 */
public record AdminUserBatchRequest(

        @NotEmpty(message = "请选择用户")
        List<@Positive(message = "用户 id 不正确") Long> ids
) {
}
