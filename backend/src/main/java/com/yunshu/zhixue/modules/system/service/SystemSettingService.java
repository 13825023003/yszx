package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.dto.SystemSettingResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import com.yunshu.zhixue.modules.system.mapper.SystemSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统设置（A-T01 ~ A-T03）：固定 4 个 config_key，按 key upsert。
 */
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingMapper systemSettingMapper;
    private final OperationLogService operationLogService;

    public SystemSettingResponse get() {
        Map<String, String> values = systemSettingMapper.selectList(
                        Wrappers.<SystemSetting>lambdaQuery())
                .stream()
                .collect(Collectors.toMap(SystemSetting::getConfigKey,
                        item -> item.getConfigValue() == null ? "" : item.getConfigValue(),
                        (left, right) -> left));
        return new SystemSettingResponse(
                valueOrDefault(values.get(SystemConstants.SETTING_SITE_NAME),
                        SystemConstants.DEFAULT_SITE_NAME),
                valueOrDefault(values.get(SystemConstants.SETTING_SITE_DESCRIPTION),
                        SystemConstants.DEFAULT_SITE_DESCRIPTION),
                values.get(SystemConstants.SETTING_LOGO_URL),
                values.get(SystemConstants.SETTING_DEFAULT_AVATAR));
    }

    @Transactional(rollbackFor = Exception.class)
    public SystemSettingResponse update(SystemSettingUpdateRequest request) {
        upsert(SystemConstants.SETTING_SITE_NAME, request.siteName().trim(), "平台名称");
        upsert(SystemConstants.SETTING_SITE_DESCRIPTION,
                trimToEmpty(request.siteDescription()), "平台描述");
        upsert(SystemConstants.SETTING_LOGO_URL, trimToNull(request.logoUrl()), "平台 Logo 对象存储 URL");
        upsert(SystemConstants.SETTING_DEFAULT_AVATAR, trimToNull(request.defaultAvatar()),
                "默认头像对象存储 URL");
        operationLogService.recordCurrent(SystemConstants.ACTION_UPDATE, "系统设置",
                "平台基本信息：" + request.siteName().trim());
        return get();
    }

    @Transactional(rollbackFor = Exception.class)
    public SystemSettingResponse reset() {
        upsert(SystemConstants.SETTING_SITE_NAME, SystemConstants.DEFAULT_SITE_NAME, "平台名称");
        upsert(SystemConstants.SETTING_SITE_DESCRIPTION,
                SystemConstants.DEFAULT_SITE_DESCRIPTION, "平台描述");
        upsert(SystemConstants.SETTING_LOGO_URL, null, "平台 Logo 对象存储 URL");
        upsert(SystemConstants.SETTING_DEFAULT_AVATAR, null, "默认头像对象存储 URL");
        operationLogService.recordCurrent(SystemConstants.ACTION_UPDATE, "系统设置",
                "恢复默认平台基本信息");
        return get();
    }

    private void upsert(String key, String value, String remark) {
        SystemSetting existing = systemSettingMapper.selectOne(Wrappers.<SystemSetting>lambdaQuery()
                .eq(SystemSetting::getConfigKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            SystemSetting setting = new SystemSetting();
            setting.setConfigKey(key);
            setting.setConfigValue(value);
            setting.setRemark(remark);
            systemSettingMapper.insert(setting);
        } else {
            systemSettingMapper.update(null, Wrappers.<SystemSetting>lambdaUpdate()
                    .eq(SystemSetting::getConfigKey, key)
                    .set(SystemSetting::getConfigValue, value));
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
