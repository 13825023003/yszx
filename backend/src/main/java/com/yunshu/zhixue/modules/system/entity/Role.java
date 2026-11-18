package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台角色（role）。角色编码全局唯一，超级管理员编码固定为 {@code super_admin}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class Role extends TokenizedLogicDeleteEntity {

    private String name;

    private String code;

    private String description;
}
