package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 保存平台基本设置（A-T02）。
 */
public record SystemSettingUpdateRequest(

        @NotBlank(message = "请输入平台名称")
        @Size(max = 30, message = "平台名称不能超过 30 个字符")
        String siteName,

        @Size(max = 200, message = "平台描述不能超过 200 个字符")
        String siteDescription,

        @Pattern(regexp = "^(|/files/.+)$", message = "Logo 仅支持站内 /files 链接")
        @Size(max = 500, message = "Logo 地址过长")
        String logoUrl,

        @Pattern(regexp = "^(|/files/.+)$", message = "默认头像仅支持站内 /files 链接")
        @Size(max = 500, message = "默认头像地址过长")
        String defaultAvatar
) {
}
