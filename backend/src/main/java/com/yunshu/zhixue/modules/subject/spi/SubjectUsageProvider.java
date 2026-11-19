package com.yunshu.zhixue.modules.subject.spi;

import java.util.Collection;
import java.util.Map;

/**
 * 学科关联内容统计（依赖倒置）。
 *
 * <p>由 course / route / interviewbank / question 等依赖 subject 的模块实现，
 * subject 模块只依赖本接口，不直接访问其他模块的表。</p>
 */
public interface SubjectUsageProvider {

    SubjectUsageType type();

    /** 按学科返回关联内容数量；默认不提供该维度统计。 */
    default Map<Long, Long> countBySubjectIds(Collection<Long> subjectIds) {
        return Map.of();
    }

    /** 全局关联内容数量；目前仅题目统计卡使用。 */
    default long countAll() {
        return 0L;
    }
}
