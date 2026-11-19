package com.yunshu.zhixue.modules.route.service;

import com.yunshu.zhixue.modules.course.spi.CourseReferenceProvider;
import com.yunshu.zhixue.modules.route.mapper.CourseRouteReferenceMapper;
import com.yunshu.zhixue.support.query.CountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 路线模块向课程模块提供课程引用数量。
 */
@Component
@RequiredArgsConstructor
public class RouteCourseReferenceProvider implements CourseReferenceProvider {

    private final CourseRouteReferenceMapper courseRouteReferenceMapper;

    @Override
    public Map<Long, Long> countActiveReferences(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        return courseRouteReferenceMapper.countByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(CountRow::getId, CountRow::getTotal));
    }
}
