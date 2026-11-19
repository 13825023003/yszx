package com.yunshu.zhixue.modules.course.service;

import com.yunshu.zhixue.modules.course.mapper.CourseUsageMapper;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageType;
import com.yunshu.zhixue.support.query.CountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程模块向学科模块提供学科关联课程数。
 */
@Component
@RequiredArgsConstructor
public class CourseSubjectUsageProvider implements SubjectUsageProvider {

    private final CourseUsageMapper courseUsageMapper;

    @Override
    public SubjectUsageType type() {
        return SubjectUsageType.COURSE;
    }

    @Override
    public Map<Long, Long> countBySubjectIds(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return courseUsageMapper.countBySubjectIds(subjectIds).stream()
                .collect(Collectors.toMap(CountRow::getId, CountRow::getTotal));
    }
}
