package com.yunshu.zhixue.common.constant;

import java.time.Duration;
import java.time.ZoneId;

/**
 * 跨模块固定常量（D13：业务阈值固定，不做后台可配置）。
 *
 * <p>数值来源：docs/云枢智学-需求文档.md §3.2、§3.8 与 docs/design/02.整体架构设计.md §2.5。</p>
 */
public final class AppConstants {

    private AppConstants() {
    }

    /** 业务时区：每日配额、日界均按上海自然日（D17）。 */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    // ==================== 分页 ====================
    public static final long DEFAULT_PAGE = 1L;
    public static final long DEFAULT_PAGE_SIZE = 10L;
    public static final long MAX_PAGE_SIZE = 100L;

    // ==================== 账号与验证码 ====================
    public static final int SMS_CODE_LENGTH = 6;
    public static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);
    public static final Duration SMS_SEND_COOLDOWN = Duration.ofSeconds(60);
    public static final int SMS_DAILY_LIMIT = 10;
    public static final int SMS_VERIFY_FAIL_LIMIT = 5;
    public static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);

    // ==================== 登录频控 ====================
    public static final int LOGIN_FAIL_LIMIT = 5;
    public static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(5);

    // ==================== AI 面试 ====================
    public static final int INTERVIEW_DAILY_QUOTA = 5;
    public static final int DEFAULT_QUESTION_COUNT = 10;
    public static final int MIN_QUESTION_COUNT = 3;
    public static final int MAX_QUESTION_COUNT = 15;
    public static final Duration INTERVIEW_ANSWER_TIMEOUT = Duration.ofMinutes(5);
    public static final Duration INTERVIEW_TEMP_TTL = Duration.ofHours(24);

    // ==================== 判分 ====================
    /** 简答题 AI 批改通过分数线（0-10 分制）。 */
    public static final int SHORT_ANSWER_PASS_SCORE = 6;

    // ==================== 看板缓存 ====================
    public static final Duration DASHBOARD_CACHE_TTL = Duration.ofMinutes(5);
}
