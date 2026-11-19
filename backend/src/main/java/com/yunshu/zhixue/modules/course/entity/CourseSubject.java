package com.yunshu.zhixue.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程-额外学科关联（course_subject）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_subject")
public class CourseSubject extends TokenizedLogicDeleteEntity {

    private Long courseId;

    private Long subjectId;
}
