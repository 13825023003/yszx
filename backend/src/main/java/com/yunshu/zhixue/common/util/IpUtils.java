package com.yunshu.zhixue.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析：Nginx 反代后优先读取 {@code X-Forwarded-For}，其次 {@code X-Real-IP}。
 *
 * <p>用于限流与操作日志记录。生产环境应确保仅信任可信代理写入的转发头。</p>
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && !UNKNOWN.equalsIgnoreCase(forwarded)) {
            // 取第一个非 unknown 的地址
            for (String part : forwarded.split(",")) {
                String candidate = part.trim();
                if (!candidate.isEmpty() && !UNKNOWN.equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !UNKNOWN.equalsIgnoreCase(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
