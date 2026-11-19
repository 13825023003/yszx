package com.yunshu.zhixue.modules.course.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.LogicDeleteUtils;
import com.yunshu.zhixue.modules.course.dto.CourseImportData;
import com.yunshu.zhixue.modules.course.dto.CourseListItemResponse;
import com.yunshu.zhixue.modules.course.dto.CourseRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStatsResponse;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.course.spi.CourseReferenceProvider;
import com.yunshu.zhixue.modules.subject.dto.SubjectBrief;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.support.audit.OperationLogRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * B站课程库（A-C01 ~ A-C05）。
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private static final int SOURCE_OTHER = 2;

    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectService subjectService;
    private final List<CourseReferenceProvider> courseReferenceProviders;
    private final OperationLogRecorder operationLogRecorder;

    public CourseStatsResponse stats() {
        long totalCourses = courseMapper.selectCount(Wrappers.<Course>lambdaQuery());
        return new CourseStatsResponse(totalCourses,
                courseMapper.selectTotalDurationMinutes(),
                courseMapper.countAssociatedSubjects());
    }

    public PageResult<CourseListItemResponse> list(String keyword, Long subjectId, Integer source,
                                                   long page, long pageSize) {
        long current = page < 1 ? AppConstants.DEFAULT_PAGE : page;
        long size = pageSize < 1
                ? AppConstants.DEFAULT_PAGE_SIZE : Math.min(pageSize, AppConstants.MAX_PAGE_SIZE);
        IPage<Course> result = courseMapper.selectPage(new Page<>(current, size),
                Wrappers.<Course>lambdaQuery()
                        .like(StringUtils.hasText(keyword), Course::getName,
                                keyword == null ? null : keyword.trim())
                        .eq(source != null, Course::getSource, source)
                        .isNull(subjectId != null && subjectId == 0, Course::getSubjectId)
                        .eq(subjectId != null && subjectId > 0, Course::getSubjectId, subjectId)
                        .orderByDesc(Course::getCreatedAt)
                        .orderByDesc(Course::getId));

        List<Long> courseIds = result.getRecords().stream().map(Course::getId).toList();
        Map<Long, List<SubjectBrief>> extraSubjects = extraSubjects(courseIds);
        Set<Long> subjectIds = new HashSet<>();
        for (Course course : result.getRecords()) {
            if (course.getSubjectId() != null) {
                subjectIds.add(course.getSubjectId());
            }
            extraSubjects.getOrDefault(course.getId(), List.of())
                    .forEach(subject -> subjectIds.add(subject.id()));
        }
        Map<Long, SubjectBrief> briefs = subjectService.briefsByIds(subjectIds);

        List<CourseListItemResponse> list = result.getRecords().stream()
                .map(course -> new CourseListItemResponse(
                        course.getId(), course.getName(), course.getCoverUrl(),
                        course.getDurationMinutes(), course.getSource(), course.getLink(),
                        course.getBvid(),
                        course.getSubjectId() == null ? null : briefs.get(course.getSubjectId()),
                        extraSubjects.getOrDefault(course.getId(), List.of())))
                .toList();
        return PageResult.of(list, result.getTotal(), current, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(CourseRequest request) {
        CourseImportData data = normalizeAndValidate(request.subjectId(), request.subjectIds(),
                request.source(), request.bvid(), request.coverUrl(), request.link(),
                request.name(), request.durationMinutes());
        ensureBvidAvailable(data.bvid(), null);
        insertCourse(data);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_CREATE, "B站课程",
                "课程「" + data.name() + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CourseRequest request) {
        Course course = requireCourse(id);
        CourseImportData data = normalizeAndValidate(request.subjectId(), request.subjectIds(),
                request.source(), request.bvid(), request.coverUrl(), request.link(),
                request.name(), request.durationMinutes());
        ensureBvidAvailable(data.bvid(), id);

        courseMapper.update(null, Wrappers.<Course>lambdaUpdate()
                .eq(Course::getId, id)
                .set(Course::getName, data.name())
                .set(Course::getCoverUrl, data.coverUrl())
                .set(Course::getDurationMinutes, data.durationMinutes())
                .set(Course::getSource, data.source())
                .set(Course::getLink, data.link())
                .set(Course::getBvid, data.bvid())
                .set(Course::getSubjectId, data.primarySubjectId()));
        replaceExtraSubjects(id, data.extraSubjectIds());
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "B站课程",
                "课程「" + data.name() + "」（原：" + course.getName() + "）");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Course course = requireCourse(id);
        long referenceCount = referenceCounts(List.of(id)).getOrDefault(id, 0L);
        if (referenceCount > 0) {
            throw new BizException(ErrorCode.CONTENT_STILL_REFERENCED,
                    "课程「" + course.getName() + "」正被 " + referenceCount
                            + " 条学习路线引用，请先从路线中移除");
        }
        courseSubjectMapper.selectList(Wrappers.<CourseSubject>lambdaQuery()
                        .eq(CourseSubject::getCourseId, id))
                .forEach(relation -> courseSubjectMapper.update(null,
                        LogicDeleteUtils.markDeleted(relation.getId())));
        courseMapper.update(null, LogicDeleteUtils.markDeleted(id));
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_DELETE, "B站课程",
                "课程「" + course.getName() + "」");
    }

    /**
     * 导入确认时按已校验数据入库；BV 唯一性由导入服务批量预检，外层统一事务。
     */
    public void createFromImport(CourseImportData data) {
        insertCourse(data);
    }

    public Map<Long, Long> referenceCounts(Collection<Long> courseIds) {
        Map<Long, Long> result = new HashMap<>();
        for (CourseReferenceProvider provider : courseReferenceProviders) {
            provider.countActiveReferences(courseIds)
                    .forEach((key, value) -> result.merge(key, value, Long::sum));
        }
        return result;
    }

    private void insertCourse(CourseImportData data) {
        Course course = new Course();
        course.setName(data.name());
        course.setCoverUrl(data.coverUrl());
        course.setDurationMinutes(data.durationMinutes());
        course.setSource(data.source());
        course.setLink(data.link());
        course.setBvid(data.bvid());
        course.setSubjectId(data.primarySubjectId());
        courseMapper.insert(course);
        for (Long subjectId : data.extraSubjectIds()) {
            CourseSubject relation = new CourseSubject();
            relation.setCourseId(course.getId());
            relation.setSubjectId(subjectId);
            courseSubjectMapper.insert(relation);
        }
    }

    private void replaceExtraSubjects(Long courseId, List<Long> extraSubjectIds) {
        courseSubjectMapper.selectList(Wrappers.<CourseSubject>lambdaQuery()
                        .eq(CourseSubject::getCourseId, courseId))
                .forEach(relation -> courseSubjectMapper.update(null,
                        LogicDeleteUtils.markDeleted(relation.getId())));
        for (Long subjectId : extraSubjectIds) {
            CourseSubject relation = new CourseSubject();
            relation.setCourseId(courseId);
            relation.setSubjectId(subjectId);
            courseSubjectMapper.insert(relation);
        }
    }

    private CourseImportData normalizeAndValidate(Long primarySubjectId, List<Long> rawExtraIds,
                                                  Integer source, String bvid, String coverUrl,
                                                  String link, String name, Integer durationMinutes) {
        String normalizedName = name.trim();
        List<Long> extraSubjectIds = rawExtraIds == null ? List.of() : rawExtraIds.stream()
                .filter(subjectId -> subjectId != null && !subjectId.equals(primarySubjectId))
                .distinct()
                .toList();
        Set<Long> subjectIds = new HashSet<>(extraSubjectIds);
        if (primarySubjectId != null) {
            subjectIds.add(primarySubjectId);
        }
        Map<Long, SubjectBrief> briefs = subjectService.briefsByIds(subjectIds);
        if (primarySubjectId != null && !briefs.containsKey(primarySubjectId)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "主学科不存在");
        }
        for (Long subjectId : extraSubjectIds) {
            if (!briefs.containsKey(subjectId)) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "额外学科不存在");
            }
        }

        String normalizedBvid = StringUtils.hasText(bvid) ? bvid.trim() : null;
        if (source == SOURCE_OTHER && normalizedBvid != null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "非 B 站课程不能填写 BV 号");
        }
        String normalizedCover = StringUtils.hasText(coverUrl) ? coverUrl.trim() : null;
        String normalizedLink = StringUtils.hasText(link) ? link.trim() : null;
        return new CourseImportData(normalizedName, durationMinutes, primarySubjectId,
                extraSubjectIds, source, normalizedLink, normalizedCover, normalizedBvid);
    }

    private void ensureBvidAvailable(String bvid, Long excludeId) {
        if (bvid == null) {
            return;
        }
        long count = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getBvid, bvid)
                .ne(excludeId != null, Course::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.NAME_ALREADY_EXISTS, "BV 号已存在：" + bvid);
        }
    }

    private Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private Map<Long, List<SubjectBrief>> extraSubjects(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<CourseSubject> relations = courseSubjectMapper.selectList(
                Wrappers.<CourseSubject>lambdaQuery().in(CourseSubject::getCourseId, courseIds));
        List<Long> subjectIds = relations.stream()
                .map(CourseSubject::getSubjectId)
                .distinct()
                .toList();
        Map<Long, SubjectBrief> briefs = subjectService.briefsByIds(subjectIds);
        Map<Long, List<SubjectBrief>> result = new HashMap<>();
        for (CourseSubject relation : relations) {
            SubjectBrief brief = briefs.get(relation.getSubjectId());
            if (brief == null) {
                continue;
            }
            result.computeIfAbsent(relation.getCourseId(), key -> new ArrayList<>()).add(brief);
        }
        return result;
    }
}
