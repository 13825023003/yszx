package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台用户（admin_user）。
 *
 * <p>属于系统管理模块；账号与手机号均有唯一键，登录时先按用户名、再按手机号匹配。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUser extends TokenizedLogicDeleteEntity {

    private String username;

    private String passwordHash;

    private String mobile;

    private String email;

    private String avatar;

    /** 角色 id，逻辑关联 role.id；单角色。 */
    private Long roleId;

    /** 1 已启用 / 0 已禁用。 */
    private Integer status;

    /** 是否内置超级管理员：1 是 / 0 否，全系统仅一个。 */
    private Integer isBuiltin;

    /** 最后登录时间（UTC）。 */
    private LocalDateTime lastLoginAt;
}
