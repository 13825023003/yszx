package com.yunshu.zhixue.modules.course.spi;

import java.util.Collection;
import java.util.Map;

/**
 * 课程被其他内容引用统计（依赖倒置），由路线等依赖 course 的模块实现。
 */
public interface CourseReferenceProvider {

    /** 按课程返回未删除关联数量，如学习路线引用数。 */
    Map<Long, Long> countActiveReferences(Collection<Long> courseIds);
}
