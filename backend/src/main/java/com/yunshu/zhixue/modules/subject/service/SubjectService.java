package com.yunshu.zhixue.modules.subject.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.util.LogicDeleteUtils;
import com.yunshu.zhixue.modules.subject.dto.StageItemResponse;
import com.yunshu.zhixue.modules.subject.dto.StageRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectListItemResponse;
import com.yunshu.zhixue.modules.subject.dto.SubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsResponse;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.subject.spi.StageUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageProvider;
import com.yunshu.zhixue.modules.subject.spi.SubjectUsageType;
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
 * 学科与阶段管理（A-S01 ~ A-S11）。
 *
 * <p>学科是内容体系根节点，不直接访问课程/题目/路线/面试题表；
 * 关联数量通过各模块实现的 SPI 汇总。</p>
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private static final int STATUS_ENABLED = 1;
    private static final String DEFAULT_STAGE_ICON = "📘";

    private final SubjectMapper subjectMapper;
    private final StageMapper stageMapper;
    private final List<SubjectUsageProvider> subjectUsageProviders;
    private final List<StageUsageProvider> stageUsageProviders;
    private final OperationLogRecorder operationLogRecorder;

    public SubjectStatsResponse stats() {
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery());
        long totalSubjects = subjectMapper.selectCount(Wrappers.<Subject>lambdaQuery());
        long subjectsWithStages = stages.stream()
                .map(Stage::getSubjectId)
                .distinct()
                .count();
        long totalQuestions = subjectUsageProviders.stream()
                .filter(provider -> provider.type() == SubjectUsageType.QUESTION)
                .mapToLong(SubjectUsageProvider::countAll)
                .sum();
        return new SubjectStatsResponse(totalSubjects, subjectsWithStages,
                stages.size(), totalQuestions);
    }

    public List<SubjectListItemResponse> list() {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .orderByAsc(Stage::getSubjectId)
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));
        Map<Long, List<Stage>> stagesBySubject = new HashMap<>();
        for (Stage stage : stages) {
            stagesBySubject.computeIfAbsent(stage.getSubjectId(), key -> new ArrayList<>())
                    .add(stage);
        }
        Map<Long, Long> stageQuestionCounts = stageQuestionCounts(stages);
        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();
        Map<Long, Long> courseCounts = usageCounts(SubjectUsageType.COURSE, subjectIds);
        Map<Long, Long> routeCounts = usageCounts(SubjectUsageType.ROUTE, subjectIds);
        Map<Long, Long> interviewCounts = usageCounts(
                SubjectUsageType.INTERVIEW_QUESTION, subjectIds);

        return subjects.stream().map(subject -> {
            List<Stage> subjectStages = stagesBySubject.getOrDefault(subject.getId(), List.of());
            List<StageItemResponse> stageItems = subjectStages.stream()
                    .map(stage -> new StageItemResponse(stage.getId(), stage.getName(),
                            stage.getIcon(), stage.getSortOrder(), stage.getStatus(),
                            stageQuestionCounts.getOrDefault(stage.getId(), 0L)))
                    .toList();
            long questionCount = stageItems.stream()
                    .mapToLong(StageItemResponse::questionCount)
                    .sum();
            long enabledStageCount = subjectStages.stream()
                    .filter(stage -> Integer.valueOf(STATUS_ENABLED).equals(stage.getStatus()))
                    .count();
            return new SubjectListItemResponse(
                    subject.getId(), subject.getName(), subject.getIcon(),
                    subject.getDescription(), subject.getThemeColor(), subject.getSortOrder(),
                    courseCounts.getOrDefault(subject.getId(), 0L),
                    routeCounts.getOrDefault(subject.getId(), 0L),
                    questionCount,
                    interviewCounts.getOrDefault(subject.getId(), 0L),
                    subjectStages.size(), enabledStageCount, stageItems);
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SubjectRequest request) {
        String name = request.name().trim();
        ensureSubjectNameAvailable(name, null);
        Subject subject = new Subject();
        subject.setName(name);
        subject.setIcon(request.icon().trim());
        subject.setDescription(request.description().trim());
        subject.setThemeColor(request.themeColor().toLowerCase());
        subject.setSortOrder(request.sortOrder());
        subjectMapper.insert(subject);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_CREATE, "学科",
                "学科「" + name + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SubjectRequest request) {
        requireSubject(id);
        String name = request.name().trim();
        ensureSubjectNameAvailable(name, id);
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        subject.setIcon(request.icon().trim());
        subject.setDescription(request.description().trim());
        subject.setThemeColor(request.themeColor().toLowerCase());
        subject.setSortOrder(request.sortOrder());
        subjectMapper.updateById(subject);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "学科",
                "学科「" + name + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortSubjects(List<Long> orderedIds) {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        validateOrder(orderedIds, subjects.stream().map(Subject::getId).toList());
        for (int index = 0; index < orderedIds.size(); index++) {
            Subject update = new Subject();
            update.setId(orderedIds.get(index));
            update.setSortOrder(index + 1);
            subjectMapper.updateById(update);
        }
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "学科", "学科排序");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Subject subject = requireSubject(id);
        List<Stage> stages = stagesOfSubject(id);
        Map<Long, Long> stageQuestionCounts = stageQuestionCounts(stages);
        long questionCount = stageQuestionCounts.values().stream().mapToLong(Long::longValue).sum();
        Map<Long, Long> courseCounts = usageCounts(SubjectUsageType.COURSE, List.of(id));
        Map<Long, Long> routeCounts = usageCounts(SubjectUsageType.ROUTE, List.of(id));
        Map<Long, Long> interviewCounts = usageCounts(
                SubjectUsageType.INTERVIEW_QUESTION, List.of(id));

        List<String> usages = new ArrayList<>();
        if (!stages.isEmpty()) {
            usages.add(stages.size() + " 个阶段");
        }
        if (questionCount > 0) {
            usages.add(questionCount + " 道题目");
        }
        long courseCount = courseCounts.getOrDefault(id, 0L);
        if (courseCount > 0) {
            usages.add(courseCount + " 门课程");
        }
        long routeCount = routeCounts.getOrDefault(id, 0L);
        if (routeCount > 0) {
            usages.add(routeCount + " 条学习路线");
        }
        long interviewCount = interviewCounts.getOrDefault(id, 0L);
        if (interviewCount > 0) {
            usages.add(interviewCount + " 道面试题");
        }
        if (!usages.isEmpty()) {
            throw new BizException(ErrorCode.CONTENT_STILL_REFERENCED,
                    "学科「" + subject.getName() + "」下存在 " + String.join("、", usages)
                            + "，请先清理关联内容");
        }
        subjectMapper.update(null, LogicDeleteUtils.markDeleted(id));
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_DELETE, "学科",
                "学科「" + subject.getName() + "」");
    }

    @Transactional(rollbackFor = Exception.class)
    public void createStage(Long subjectId, StageRequest request) {
        Subject subject = requireSubject(subjectId);
        String name = request.name().trim();
        ensureStageNameAvailable(subjectId, name, null);
        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        stage.setName(name);
        stage.setIcon(StringUtils.hasText(request.icon())
                ? request.icon().trim() : DEFAULT_STAGE_ICON);
        stage.setSortOrder(request.sortOrder() != null
                ? request.sortOrder() : nextStageSortOrder(subjectId));
        stage.setStatus(STATUS_ENABLED);
        stageMapper.insert(stage);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_CREATE, "阶段",
                "阶段「" + name + "」（学科：" + subject.getName() + "）");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long subjectId, Long stageId, StageRequest request) {
        Subject subject = requireSubject(subjectId);
        Stage stage = requireStage(subjectId, stageId);
        String name = request.name().trim();
        ensureStageNameAvailable(subjectId, name, stageId);

        Stage update = new Stage();
        update.setId(stageId);
        update.setName(name);
        update.setIcon(StringUtils.hasText(request.icon())
                ? request.icon().trim() : DEFAULT_STAGE_ICON);
        if (request.sortOrder() != null) {
            update.setSortOrder(request.sortOrder());
        }
        stageMapper.updateById(update);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "阶段",
                "阶段「" + name + "」（学科：" + subject.getName() + "）");
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortStages(Long subjectId, List<Long> orderedIds) {
        Subject subject = requireSubject(subjectId);
        List<Stage> stages = stagesOfSubject(subjectId);
        validateOrder(orderedIds, stages.stream().map(Stage::getId).toList());
        for (int index = 0; index < orderedIds.size(); index++) {
            Stage update = new Stage();
            update.setId(orderedIds.get(index));
            update.setSortOrder(index + 1);
            stageMapper.updateById(update);
        }
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_UPDATE, "阶段",
                "阶段排序（学科：" + subject.getName() + "）");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStageStatus(Long subjectId, Long stageId, Integer status) {
        Subject subject = requireSubject(subjectId);
        Stage stage = requireStage(subjectId, stageId);
        Stage update = new Stage();
        update.setId(stageId);
        update.setStatus(status);
        stageMapper.updateById(update);
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_STATUS, "阶段",
                "阶段「" + stage.getName() + "」（学科：" + subject.getName() + "）");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStage(Long subjectId, Long stageId) {
        Subject subject = requireSubject(subjectId);
        Stage stage = requireStage(subjectId, stageId);
        long questionCount = stageQuestionCounts(List.of(stage)).getOrDefault(stageId, 0L);
        if (questionCount > 0) {
            throw new BizException(ErrorCode.CONTENT_STILL_REFERENCED,
                    "阶段「" + stage.getName() + "」下关联 " + questionCount
                            + " 道题目，请先删除或迁移题目");
        }
        stageMapper.update(null, LogicDeleteUtils.markDeleted(stageId));
        operationLogRecorder.recordCurrent(OperationLogRecorder.ACTION_DELETE, "阶段",
                "阶段「" + stage.getName() + "」（学科：" + subject.getName() + "）");
    }

    private Subject requireSubject(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学科不存在");
        }
        return subject;
    }

    private Stage requireStage(Long subjectId, Long stageId) {
        Stage stage = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getId, stageId)
                .eq(Stage::getSubjectId, subjectId)
                .last("LIMIT 1"));
        if (stage == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "阶段不存在");
        }
        return stage;
    }

    private List<Stage> stagesOfSubject(Long subjectId) {
        return stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));
    }

    private Map<Long, Long> stageQuestionCounts(Collection<Stage> stages) {
        if (stages.isEmpty()) {
            return Map.of();
        }
        List<Long> stageIds = stages.stream().map(Stage::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (StageUsageProvider provider : stageUsageProviders) {
            provider.countByStageIds(stageIds)
                    .forEach((key, value) -> result.merge(key, value, Long::sum));
        }
        return result;
    }

    private Map<Long, Long> usageCounts(SubjectUsageType type, Collection<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (SubjectUsageProvider provider : subjectUsageProviders) {
            if (provider.type() != type) {
                continue;
            }
            provider.countBySubjectIds(subjectIds)
                    .forEach((key, value) -> result.merge(key, value, Long::sum));
        }
        return result;
    }

    private void ensureSubjectNameAvailable(String name, Long excludeId) {
        long count = subjectMapper.selectCount(Wrappers.<Subject>lambdaQuery()
                .eq(Subject::getName, name)
                .ne(excludeId != null, Subject::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.NAME_ALREADY_EXISTS, "学科名称已存在");
        }
    }

    private void ensureStageNameAvailable(Long subjectId, String name, Long excludeId) {
        long count = stageMapper.selectCount(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getName, name)
                .ne(excludeId != null, Stage::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.NAME_ALREADY_EXISTS, "该学科下阶段名称已存在");
        }
    }

    private int nextStageSortOrder(Long subjectId) {
        Stage last = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .orderByDesc(Stage::getSortOrder)
                .orderByDesc(Stage::getId)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private void validateOrder(List<Long> orderedIds, List<Long> actualIds) {
        Set<Long> ordered = new HashSet<>(orderedIds);
        Set<Long> actual = new HashSet<>(actualIds);
        if (ordered.size() != orderedIds.size()
                || actual.size() != ordered.size()
                || !actual.equals(ordered)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "排序数据不完整，请刷新后重试");
        }
    }
}
