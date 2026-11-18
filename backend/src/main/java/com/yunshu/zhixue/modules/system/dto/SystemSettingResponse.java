package com.yunshu.zhixue.modules.system.dto;

/**
 * 平台基本设置（A-T01）。
 */
public record SystemSettingResponse(String siteName,
                                    String siteDescription,
                                    String logoUrl,
                                    String defaultAvatar) {
}
