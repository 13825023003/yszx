package com.yunshu.zhixue.modules.system;

import java.util.Set;

/**
 * 系统管理模块固定常量。
 */
public final class SystemConstants {

    private SystemConstants() {
    }

    public static final String SUPER_ADMIN_ROLE_CODE = "super_admin";
    public static final String WILDCARD_PERMISSION = "*";

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
    public static final int BUILTIN_YES = 1;

    public static final String ACTION_LOGIN = "登录";
    public static final String ACTION_LOGOUT = "登出";
    public static final String ACTION_CREATE = "新增";
    public static final String ACTION_UPDATE = "修改";
    public static final String ACTION_DELETE = "删除";
    public static final String ACTION_STATUS = "禁用启用";
    public static final String ACTION_EXPORT = "导出";
    public static final String ACTION_OTHER = "其他";

    public static final Set<String> LOG_ACTIONS = Set.of(
            ACTION_LOGIN, ACTION_LOGOUT, ACTION_CREATE, ACTION_UPDATE,
            ACTION_DELETE, ACTION_STATUS, ACTION_EXPORT, ACTION_OTHER);

    public static final String SETTING_SITE_NAME = "site_name";
    public static final String SETTING_SITE_DESCRIPTION = "site_description";
    public static final String SETTING_LOGO_URL = "logo_url";
    public static final String SETTING_DEFAULT_AVATAR = "default_avatar";

    public static final String DEFAULT_SITE_NAME = "云枢智学";
    public static final String DEFAULT_SITE_DESCRIPTION = "AI 赋能的智能面试训练平台";
}
