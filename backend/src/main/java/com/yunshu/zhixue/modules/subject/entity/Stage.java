package com.yunshu.zhixue.modules.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学科阶段（stage）：同学科下名称唯一，题目挂载在阶段下。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stage")
public class Stage extends TokenizedLogicDeleteEntity {

    private Long subjectId;

    private String name;

    /** 留空时前端按默认图标展示。 */
    private String icon;

    private Integer sortOrder;

    /** 1 启用 / 0 禁用。 */
    private Integer status;
}
