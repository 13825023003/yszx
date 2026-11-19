package com.yunshu.zhixue.modules.course.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmRequest;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewResponse;
import com.yunshu.zhixue.modules.course.dto.CourseListItemResponse;
import com.yunshu.zhixue.modules.course.dto.CourseRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStatsResponse;
import com.yunshu.zhixue.modules.course.service.CourseExcelImportService;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * B站课程库（A-C01 ~ A-C07）。
 */
@Tag(name = "B站课程库")
@RestController
@RequestMapping("/api/admin/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseExcelImportService courseExcelImportService;

    @Operation(summary = "课程统计卡")
    @GetMapping("/stats")
    @RequirePermission("course:view")
    public ApiResponse<CourseStatsResponse> stats() {
        return ApiResponse.ok(courseService.stats());
    }

    @Operation(summary = "课程列表")
    @GetMapping
    @RequirePermission("course:view")
    public ApiResponse<PageResult<CourseListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer source,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(courseService.list(keyword, subjectId, source, page, pageSize));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    @RequirePermission("course:add")
    public ApiResponse<Void> create(@Valid @RequestBody CourseRequest request) {
        courseService.create(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "编辑课程")
    @PutMapping("/{id}")
    @RequirePermission("course:edit")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody CourseRequest request) {
        courseService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    @RequirePermission("course:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Excel 导入预览")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("course:add")
    public ApiResponse<CourseImportPreviewResponse> preview(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(courseExcelImportService.preview(file));
    }

    @Operation(summary = "Excel 导入确认")
    @PostMapping("/import/confirm")
    @RequirePermission("course:add")
    public ApiResponse<CourseImportConfirmResponse> confirm(
            @Valid @RequestBody CourseImportConfirmRequest request) {
        return ApiResponse.ok(courseExcelImportService.confirm(request.token()));
    }
}
