package com.yunshu.zhixue.modules.subject.spi;

import java.util.Collection;
import java.util.Map;

/**
 * 阶段关联内容统计（依赖倒置），由题模块实现。
 */
public interface StageUsageProvider {

    /** 按阶段返回题目数量。 */
    Map<Long, Long> countByStageIds(Collection<Long> stageIds);
}
