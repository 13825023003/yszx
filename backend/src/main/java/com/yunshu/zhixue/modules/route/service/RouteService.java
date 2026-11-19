package com.yunshu.zhixue.modules.route.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.util.LogicDeleteUtils;
import com.yunshu.zhixue.modules.course.dto.CourseListItemResponse;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.modules.route.dto.RouteCourseItemResponse;
import com.yunshu.zhixue.modules.route.dto.RouteCreateResponse;
import com.yunshu.zhixue.modules.route.dto.RouteDetailResponse;
import com.yunshu.zhixue.modules.route.dto.RouteListItemResponse;
import com.yunshu.zhixue.modules.route.dto.RouteRequest;
import com.yunshu.zhixue.modules.route.entity.LearningRoute;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import com.yunshu.zhixue.modules.route.mapper.LearningRouteMapper;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseMapper;
import com.yunshu.zhixue.modules.subject.dto.SubjectBrief;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.support.audit.OperationLogRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习路线管理（A-R01 ~ A-R09）。
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    private static final int STATUS_ENABLED = 1;

    private final LearningRouteMapper routeMapper;
    private final RouteCourseMapper routeCourseMapper;
    private final CourseService courseService;
    private final SubjectService subjectService;
    private final OperationLogRecorder operationLogRecorder;

    public List<RouteListItemResponse> list() {
        List<LearningRoute> routes = routeMapper.selectList(Wrappers.<LearningRoute>lambdaQuery()
                .orderByDesc(LearningRoute::getCreatedAt)
                .orderByDesc(LearningRoute::getId));
        List<RouteCourse> relations = routeCourseMapper.selectList(
                Wrappers.<RouteCourse>lambdaQuery()
                        .orderByAsc(RouteCourse::getRouteId)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));
        List<Long> courseIds = relations.stream().map(RouteCourse::getCourseId).distinct().toList();
        Map<Long, CourseListItemResponse> courseDetails = courseService.detailsByIds(courseIds);
        Set<Long> subjectIds = routes.stream().map(LearningRoute::getSubjectId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, SubjectBrief> subjects = subjectService.briefsByIds(subjectIds);
        Map<Long, List<RouteCourse>> relationsByRoute = new HashMap<>();
        for (RouteCourse relation : relations) {
            relationsByRoute.computeIfAbsent(relation.getRouteId(), key -> new ArrayList<>())
                    .add(relation);
        }

        return routes.stream().map(route -> {
            List<RouteCourseItemResponse> items = courseItems(
                    relationsByRoute.getOrDefault(route.getId(), List.of()),
                    courseDetails, route.getSubjectId());
            long totalDuration = items.stream()
                    .mapToLong(item -> item.durationMinutes() == null ? 0 : item.durationMinutes())
                    .sum();
            SubjectBrief subject = subjects.get(route.getSubjectId());
            return new RouteListItemResponse(
                    route.getId(), route.getName(), route.getSubjectId(),
                    subject == null ? "未知学科" : subject.name(), route.getStatus(),
                    route.getIcon(), route.getTargetEducation(), route.getDescription(),
                    items.size(), totalDuration,
                    items.stream().limit(4).toList());
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public RouteCreateResponse create(RouteRequest request) {
        requireSubject(request.subjectId());
        LearningRoute route = new LearningRoute();
        route.setName(request.name().trim());
        route.setSubjectId(request.subjectId());
        route.setStatus(request.status() == null ? STATUS_ENABLED : request.status());
        route.setIcon(trimToNull(request.icon()));
        route.setTargetEducation(trimToNull(request.targetEducation()));
        route.setDescription(trimToNull(request.description()));
        routeMapper.insert(route);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_CREATE, "学习路线",
                "路线「" + route.getName() + "」");
        return new RouteCreateResponse(route.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteRequest request) {
        LearningRoute route = requireRoute(id);
        requireSubject(request.subjectId());
        routeMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, id)
                .set(LearningRoute::getName, request.name().trim())
                .set(LearningRoute::getSubjectId, request.subjectId())
                .set(LearningRoute::getStatus,
                        request.status() == null ? route.getStatus() : request.status())
                .set(LearningRoute::getIcon, trimToNull(request.icon()))
                .set(LearningRoute::getTargetEducation, trimToNull(request.targetEducation()))
                .set(LearningRoute::getDescription, trimToNull(request.description())));
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "学习路线",
                "路线「" + request.name().trim() + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        LearningRoute route = requireRoute(id);
        routeMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, id)
                .set(LearningRoute::getStatus, status));
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_STATUS, "学习路线",
                "路线「" + route.getName() + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LearningRoute route = requireRoute(id);
        routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                        .eq(RouteCourse::getRouteId, id))
                .forEach(relation -> routeCourseMapper.update(null,
                        LogicDeleteUtils.markDeleted(relation.getId())));
        routeMapper.deleteById(id);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_DELETE, "学习路线",
                "路线「" + route.getName() + "」");
    }

    public RouteDetailResponse detail(Long id) {
        LearningRoute route = requireRoute(id);
        List<RouteCourse> relations = routeCourseMapper.selectList(
                Wrappers.<RouteCourse>lambdaQuery()
                        .eq(RouteCourse::getRouteId, id)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));
        List<Long> courseIds = relations.stream().map(RouteCourse::getCourseId).distinct().toList();
        Map<Long, CourseListItemResponse> courseDetails = courseService.detailsByIds(courseIds);
        List<RouteCourseItemResponse> items = courseItems(relations, courseDetails,
                route.getSubjectId());
        long totalDuration = items.stream()
                .mapToLong(item -> item.durationMinutes() == null ? 0 : item.durationMinutes())
                .sum();
        SubjectBrief subject = subjectService.briefsByIds(List.of(route.getSubjectId()))
                .get(route.getSubjectId());
        return new RouteDetailResponse(
                route.getId(), route.getName(), route.getSubjectId(),
                subject == null ? "未知学科" : subject.name(), route.getStatus(),
                route.getIcon(), route.getTargetEducation(), route.getDescription(),
                items.size(), totalDuration, items);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addCourses(Long id, List<Long> courseIds) {
        LearningRoute route = requireRoute(id);
        List<Long> ids = courseIds.stream().distinct().toList();
        Map<Long, CourseListItemResponse> details = courseService.detailsByIds(ids);
        if (details.size() != ids.size()) {
            throw new BizException(ErrorCode.NOT_FOUND, "部分课程不存在或已删除");
        }
        Set<Long> existing = routeCourseMapper.selectList(Wrappers.<RouteCourse>lambdaQuery()
                        .eq(RouteCourse::getRouteId, id))
                .stream()
                .map(RouteCourse::getCourseId)
                .collect(java.util.stream.Collectors.toSet());
        int nextSort = nextSortOrder(id);
        for (Long courseId : ids) {
            if (existing.contains(courseId)) {
                continue;
            }
            RouteCourse relation = new RouteCourse();
            relation.setRouteId(id);
            relation.setCourseId(courseId);
            relation.setSortOrder(nextSort++);
            routeCourseMapper.insert(relation);
        }
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "学习路线",
                "路线「" + route.getName() + "」添加课程 " + ids.size() + " 门");
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeCourse(Long id, Long courseId) {
        LearningRoute route = requireRoute(id);
        RouteCourse relation = routeCourseMapper.selectOne(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, id)
                .eq(RouteCourse::getCourseId, courseId)
                .last("LIMIT 1"));
        if (relation == null) {
            return;
        }
        routeCourseMapper.update(null, LogicDeleteUtils.markDeleted(relation.getId()));
        compactSortOrder(id);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "学习路线",
                "路线「" + route.getName() + "」移除课程");
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortCourses(Long id, List<Long> courseIds) {
        LearningRoute route = requireRoute(id);
        List<RouteCourse> relations = routeCourseMapper.selectList(
                Wrappers.<RouteCourse>lambdaQuery()
                        .eq(RouteCourse::getRouteId, id)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));
        Set<Long> expected = relations.stream().map(RouteCourse::getCourseId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> ordered = new LinkedHashSet<>(courseIds);
        if (ordered.size() != courseIds.size()
                || expected.size() != ordered.size()
                || !expected.equals(ordered)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "课程排序数据不完整，请刷新后重试");
        }
        for (int index = 0; index < courseIds.size(); index++) {
            RouteCourse update = new RouteCourse();
            update.setRouteId(id);
            update.setCourseId(courseIds.get(index));
            update.setSortOrder(index + 1);
            routeCourseMapper.update(update, Wrappers.<RouteCourse>lambdaUpdate()
                    .eq(RouteCourse::getRouteId, id)
                    .eq(RouteCourse::getCourseId, courseIds.get(index)));
        }
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "学习路线",
                "路线「" + route.getName() + "」课程排序");
    }

    private List<RouteCourseItemResponse> courseItems(List<RouteCourse> relations,
                                                       Map<Long, CourseListItemResponse> details,
                                                       Long routeSubjectId) {
        List<RouteCourseItemResponse> items = new ArrayList<>();
        for (RouteCourse relation : relations) {
            CourseListItemResponse course = details.get(relation.getCourseId());
            if (course == null) {
                continue;
            }
            boolean local = course.primarySubject() != null
                    && course.primarySubject().id().equals(routeSubjectId);
            items.add(new RouteCourseItemResponse(
                    course.id(), course.name(), course.coverUrl(), course.durationMinutes(),
                    course.source(), course.link(), course.bvid(), course.primarySubject(),
                    course.subjects(), relation.getSortOrder(), local));
        }
        return items;
    }

    private void compactSortOrder(Long routeId) {
        List<RouteCourse> relations = routeCourseMapper.selectList(
                Wrappers.<RouteCourse>lambdaQuery()
                        .eq(RouteCourse::getRouteId, routeId)
                        .orderByAsc(RouteCourse::getSortOrder)
                        .orderByAsc(RouteCourse::getId));
        for (int index = 0; index < relations.size(); index++) {
            RouteCourse update = new RouteCourse();
            update.setId(relations.get(index).getId());
            update.setSortOrder(index + 1);
            routeCourseMapper.updateById(update);
        }
    }

    private int nextSortOrder(Long routeId) {
        RouteCourse last = routeCourseMapper.selectOne(Wrappers.<RouteCourse>lambdaQuery()
                .eq(RouteCourse::getRouteId, routeId)
                .orderByDesc(RouteCourse::getSortOrder)
                .orderByDesc(RouteCourse::getId)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private LearningRoute requireRoute(Long id) {
        LearningRoute route = routeMapper.selectById(id);
        if (route == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习路线不存在");
        }
        return route;
    }

    private void requireSubject(Long subjectId) {
        if (!subjectService.exists(subjectId)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "关联学科不存在");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
