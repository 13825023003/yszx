package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增 / 编辑课程（A-C03 / A-C04）。
 */
public record CourseRequest(

        @NotBlank(message = "请输入课程名称")
        @Size(max = 100, message = "课程名称不能超过 100 个字符")
        String name,

        @Pattern(regexp = "^(https?://.+|/files/.+)?$", message = "封面需为 http(s) 或站内 /files 链接")
        @Size(max = 500, message = "封面地址过长")
        String coverUrl,

        @NotNull(message = "请输入课程时长")
        @Min(value = 1, message = "课程时长需大于 0 分钟")
        @Max(value = 1000000, message = "课程时长过大")
        Integer durationMinutes,

        @Positive(message = "主学科不正确")
        Long subjectId,

        @Size(max = 20, message = "额外学科数量不正确")
        List<@Positive(message = "额外学科不正确") Long> subjectIds,

        @NotNull(message = "请选择课程来源")
        @Min(value = 1, message = "课程来源不正确")
        @Max(value = 2, message = "课程来源不正确")
        Integer source,

        @Pattern(regexp = "^(https?://.+)?$", message = "课程链接需以 http(s):// 开头")
        @Size(max = 500, message = "课程链接过长")
        String link,

        @Pattern(regexp = "^(BV[0-9A-Za-z]{8,18})?$", message = "BV 号格式不正确")
        @Size(max = 20, message = "BV 号过长")
        String bvid
) {
}
