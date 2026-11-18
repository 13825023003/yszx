package com.yunshu.zhixue.modules.system.dto;

/**
 * 重置密码结果（A-M07）：默认密码仅在此响应中明文返回一次。
 */
public record ResetPasswordResponse(String password) {
}
