package com.yunshu.zhixue.modules.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学科（subject）：内容体系根节点，同时作为 AI 面试方向。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subject")
public class Subject extends TokenizedLogicDeleteEntity {

    private String name;

    /** Emoji 图标。 */
    private String icon;

    private String description;

    /** 预置色板中的十六进制主题色。 */
    private String themeColor;

    private Integer sortOrder;
}
