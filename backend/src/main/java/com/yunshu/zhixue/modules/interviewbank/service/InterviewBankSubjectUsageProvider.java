package com.yunshu.zhixue.modules.interviewbank.service;

import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionUsageMapper;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageType;
import com.yunshu.zhixue.support.query.CountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 面试题库向学科模块提供学科关联面试题数。
 */
@Component
@RequiredArgsConstructor
public class InterviewBankSubjectUsageProvider implements SubjectUsageProvider {

    private final InterviewQuestionUsageMapper interviewQuestionUsageMapper;

    @Override
    public SubjectUsageType type() {
        return SubjectUsageType.INTERVIEW_QUESTION;
    }

    @Override
    public Map<Long, Long> countBySubjectIds(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return interviewQuestionUsageMapper.countBySubjectIds(subjectIds).stream()
                .collect(Collectors.toMap(CountRow::getId, CountRow::getTotal));
    }
}
