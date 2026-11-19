package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.LogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学习路线（learning_route）：无 deleted_token，逻辑删除即可。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_route")
public class LearningRoute extends LogicDeleteEntity {

    private String name;

    private Long subjectId;

    /** 1 启用 / 0 禁用。 */
    private Integer status;

    private String icon;

    private String targetEducation;

    private String description;
}
