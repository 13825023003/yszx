package com.yunshu.zhixue.modules.route.service;

import com.yunshu.zhixue.modules.route.mapper.RouteUsageMapper;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageType;
import com.yunshu.zhixue.support.query.CountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 路线模块向学科模块提供学科关联路线数。
 */
@Component
@RequiredArgsConstructor
public class RouteSubjectUsageProvider implements SubjectUsageProvider {

    private final RouteUsageMapper routeUsageMapper;

    @Override
    public SubjectUsageType type() {
        return SubjectUsageType.ROUTE;
    }

    @Override
    public Map<Long, Long> countBySubjectIds(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return routeUsageMapper.countBySubjectIds(subjectIds).stream()
                .collect(Collectors.toMap(CountRow::getId, CountRow::getTotal));
    }
}
