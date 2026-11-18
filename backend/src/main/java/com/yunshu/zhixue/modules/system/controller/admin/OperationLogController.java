package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.OperationLogResponse;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 操作日志（A-G01 ~ A-G04）。
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/admin/v1/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "日志列表")
    @GetMapping
    @RequirePermission("system:log_view")
    public ApiResponse<PageResult<OperationLogResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(operationLogService.list(keyword, operationType,
                startDate, endDate, page, pageSize));
    }

    @Operation(summary = "日志详情")
    @GetMapping("/{id}")
    @RequirePermission("system:log_view")
    public ApiResponse<OperationLogResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(operationLogService.detail(id));
    }

    @Operation(summary = "导出日志 CSV")
    @GetMapping("/export")
    @RequirePermission("system:log_view")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        OperationLogService.ExportResult result = operationLogService.export(
                keyword, operationType, startDate, endDate);
        String filename = "operation_logs_"
                + LocalDate.now(AppConstants.BUSINESS_ZONE) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename).build());
        headers.setContentLength(result.content().length);
        return new ResponseEntity<>(result.content(), headers, HttpStatus.OK);
    }

    @Operation(summary = "清空日志")
    @DeleteMapping
    @RequirePermission("system:log_view")
    public ApiResponse<Void> clear() {
        operationLogService.clear();
        return ApiResponse.ok();
    }
}
