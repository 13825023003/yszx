package com.yunshu.zhixue.common.util;

import com.yunshu.zhixue.common.constant.AppConstants;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 业务日期工具：所有「今日」「每日上限」「次日 0 点重置」均按 Asia/Shanghai 自然日（D17）。
 *
 * <p>数据库仍统一存 UTC；本工具只负责业务日界与 Redis TTL 计算。</p>
 */
public final class BusinessDateUtils {

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BusinessDateUtils() {
    }

    /** 当前业务日（上海），如 20261113。 */
    public static String todayKey() {
        return LocalDate.now(AppConstants.BUSINESS_ZONE).format(DAY_KEY);
    }

    /** 距上海次日 0 点的剩余时长，用于每日配额/验证码日计数的 Redis TTL。 */
    public static Duration ttlUntilNextBusinessMidnight() {
        ZonedDateTime nextMidnight = LocalDate.now(AppConstants.BUSINESS_ZONE)
                .plusDays(1)
                .atStartOfDay(AppConstants.BUSINESS_ZONE);
        return Duration.between(Instant.now(), nextMidnight.toInstant());
    }

    /** 数据库 UTC 时间转业务时区（上海）时间，用于接口展示。 */
    public static LocalDateTime toBusinessDateTime(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return utc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(AppConstants.BUSINESS_ZONE)
                .toLocalDateTime();
    }

    /** 上海自然日 0 点转 UTC，用于按业务日期筛选 UTC 存储的时间字段。 */
    public static LocalDateTime businessDayStartToUtc(LocalDate date) {
        return date.atStartOfDay(AppConstants.BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }
}
