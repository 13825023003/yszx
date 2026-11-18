package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统设置（system_setting）：固定 4 个 config_key，按 key upsert。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_setting")
public class SystemSetting extends BaseEntity {

    private String configKey;

    private String configValue;

    private String remark;
}
