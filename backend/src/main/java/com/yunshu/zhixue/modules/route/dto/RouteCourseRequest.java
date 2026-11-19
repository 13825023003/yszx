package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 添加课程 / 课程排序（A-R07 / A-R09）。
 */
public record RouteCourseRequest(

        @NotEmpty(message = "请选择课程")
        @Size(max = 500, message = "课程数量不正确")
        List<@Positive(message = "课程 id 不正确") Long> courseIds
) {
}
