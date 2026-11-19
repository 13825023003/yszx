package com.yunshu.zhixue.modules.question.service;

import com.yunshu.zhixue.modules.question.mapper.QuestionUsageMapper;
import com.yunshu.zhixue.modules.subject.spi.StageUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageType;
import com.yunshu.zhixue.support.query.CountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题模块向学科模块提供阶段题目数量与题目总数。
 */
@Component
@RequiredArgsConstructor
public class QuestionSubjectUsageProvider implements SubjectUsageProvider, StageUsageProvider {

    private final QuestionUsageMapper questionUsageMapper;

    @Override
    public SubjectUsageType type() {
        return SubjectUsageType.QUESTION;
    }

    @Override
    public long countAll() {
        return questionUsageMapper.countAll();
    }

    @Override
    public Map<Long, Long> countByStageIds(Collection<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return Map.of();
        }
        return questionUsageMapper.countByStageIds(stageIds).stream()
                .collect(Collectors.toMap(CountRow::getId, CountRow::getTotal));
    }
}
