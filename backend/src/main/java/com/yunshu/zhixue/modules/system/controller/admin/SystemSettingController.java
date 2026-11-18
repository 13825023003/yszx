package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置（A-T01 ~ A-T03）。
 */
@Tag(name = "系统设置")
@RestController
@RequestMapping("/api/admin/v1/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @Operation(summary = "获取平台设置（登录即可）")
    @GetMapping
    public ApiResponse<SystemSettingResponse> get() {
        return ApiResponse.ok(systemSettingService.get());
    }

    @Operation(summary = "保存平台设置")
    @PutMapping
    @RequirePermission("system:setting")
    public ApiResponse<SystemSettingResponse> update(
            @Valid @RequestBody SystemSettingUpdateRequest request) {
        return ApiResponse.ok(systemSettingService.update(request));
    }

    @Operation(summary = "恢复默认设置")
    @PostMapping("/reset")
    @RequirePermission("system:setting")
    public ApiResponse<SystemSettingResponse> reset() {
        return ApiResponse.ok(systemSettingService.reset());
    }
}
