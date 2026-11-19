package com.yunshu.zhixue.modules.subject.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.subject.dto.SortRequest;
import com.yunshu.zhixue.modules.subject.dto.StageRequest;
import com.yunshu.zhixue.modules.subject.dto.StageStatusRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectListItemResponse;
import com.yunshu.zhixue.modules.subject.dto.SubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsResponse;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学科与阶段管理（A-S01 ~ A-S11）。
 */
@Tag(name = "学科与阶段管理")
@RestController
@RequestMapping("/api/admin/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @Operation(summary = "学科统计卡")
    @GetMapping("/stats")
    @RequirePermission("subject:view")
    public ApiResponse<SubjectStatsResponse> stats() {
        return ApiResponse.ok(subjectService.stats());
    }

    @Operation(summary = "学科卡片列表")
    @GetMapping
    @RequirePermission("subject:view")
    public ApiResponse<List<SubjectListItemResponse>> list() {
        return ApiResponse.ok(subjectService.list());
    }

    @Operation(summary = "新增学科")
    @PostMapping
    @RequirePermission("subject:add")
    public ApiResponse<Void> create(@Valid @RequestBody SubjectRequest request) {
        subjectService.create(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑学科")
    @PutMapping("/{id}")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody SubjectRequest request) {
        subjectService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "学科排序")
    @PutMapping("/sort")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> sort(@Valid @RequestBody SortRequest request) {
        subjectService.sortSubjects(request.orderedIds());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除学科")
    @DeleteMapping("/{id}")
    @RequirePermission("subject:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "新增阶段")
    @PostMapping("/{subjectId}/stages")
    @RequirePermission("subject:add")
    public ApiResponse<Void> createStage(@PathVariable Long subjectId,
                                         @Valid @RequestBody StageRequest request) {
        subjectService.createStage(subjectId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑阶段")
    @PutMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> updateStage(@PathVariable Long subjectId,
                                         @PathVariable Long id,
                                         @Valid @RequestBody StageRequest request) {
        subjectService.updateStage(subjectId, id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "阶段排序")
    @PutMapping("/{subjectId}/stages/sort")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> sortStages(@PathVariable Long subjectId,
                                        @Valid @RequestBody SortRequest request) {
        subjectService.sortStages(subjectId, request.orderedIds());
        return ApiResponse.ok();
    }

    @Operation(summary = "阶段启用/禁用")
    @PutMapping("/{subjectId}/stages/{id}/status")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> updateStageStatus(@PathVariable Long subjectId,
                                               @PathVariable Long id,
                                               @Valid @RequestBody StageStatusRequest request) {
        subjectService.updateStageStatus(subjectId, id, request.status());
        return ApiResponse.ok();
    }

    @Operation(summary = "删除阶段")
    @DeleteMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:delete")
    public ApiResponse<Void> deleteStage(@PathVariable Long subjectId,
                                         @PathVariable Long id) {
        subjectService.deleteStage(subjectId, id);
        return ApiResponse.ok();
    }
}
