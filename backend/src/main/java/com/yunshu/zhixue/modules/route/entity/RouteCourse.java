package com.yunshu.zhixue.modules.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 路线-课程关联（route_course），sort_order 即学习顺序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("route_course")
public class RouteCourse extends TokenizedLogicDeleteEntity {

    private Long routeId;

    private Long courseId;

    private Integer sortOrder;
}
