package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 新增后台用户（A-M02）。
 */
public record AdminUserCreateRequest(

        @NotBlank(message = "请输入用户名")
        @Size(min = 2, max = 50, message = "用户名长度需为 2~50 个字符")
        String username,

        @NotBlank(message = "请输入登录密码")
        @Size(min = 6, max = 72, message = "密码长度需为 6~72 个字符")
        String password,

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的 11 位手机号")
        String mobile,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
        String email,

        @Pattern(regexp = "^(|/files/.+)$", message = "头像仅支持站内 /files 链接")
        @Size(max = 500, message = "头像地址过长")
        String avatar,

        @NotNull(message = "请选择角色")
        @Positive(message = "角色不正确")
        Long roleId,

        @Min(value = 0, message = "状态不正确")
        @Max(value = 1, message = "状态不正确")
        Integer status
) {
}
