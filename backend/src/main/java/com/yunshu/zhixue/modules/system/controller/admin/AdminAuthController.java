package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.system.dto.AdminProfileResponse;
import com.yunshu.zhixue.modules.system.dto.TokenPairResponse;
import com.yunshu.zhixue.modules.system.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口（A-A01 ~ A-A03）。
 */
@Tag(name = "管理端认证")
@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "后台登录")
    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @Operation(summary = "当前管理员信息")
    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me() {
        return ApiResponse.ok(adminAuthService.me());
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        return ApiResponse.ok(adminAuthService.refresh(refreshToken));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        adminAuthService.logout(authorization, refreshToken);
        return ApiResponse.ok();
    }
}
