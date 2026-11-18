package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端登录入参（A-A01）：用户名或手机号 + 密码。
 */
public record AdminLoginRequest(

        @NotBlank(message = "请输入用户名或手机号")
        @Size(max = 100, message = "账号长度不能超过 100 个字符")
        String account,

        @NotBlank(message = "请输入密码")
        @Size(max = 100, message = "密码长度不能超过 100 个字符")
        String password
) {
}
