package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 学科 / 阶段排序（A-S05 / A-S09）：按数组顺序重排。
 */
public record SortRequest(

        @NotEmpty(message = "排序数据不能为空")
        List<@Positive(message = "排序 id 不正确") Long> orderedIds
) {
}
