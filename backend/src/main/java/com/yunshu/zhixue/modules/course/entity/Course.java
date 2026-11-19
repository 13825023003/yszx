package com.yunshu.zhixue.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * B站课程库（course）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends TokenizedLogicDeleteEntity {

    private String name;

    private String coverUrl;

    private Integer durationMinutes;

    /** 1 B站 / 2 其他。 */
    private Integer source;

    private String link;

    /** BV 号；非 B 站来源为空。 */
    private String bvid;

    /** 主学科；NULL = 其他学科。 */
    private Long subjectId;
}
