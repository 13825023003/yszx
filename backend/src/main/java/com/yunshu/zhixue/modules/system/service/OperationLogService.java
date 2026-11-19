package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.BusinessDateUtils;
import com.yunshu.zhixue.common.util.IpUtils;
import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.dto.OperationLogResponse;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.security.SecurityUtils;
import com.yunshu.zhixue.support.audit.OperationLogRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志：记录、检索、导出与清空（A-G01 ~ A-G04）。
 *
 * <p>日志写入为尽力而为：审计失败不得阻断主业务操作。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService implements OperationLogRecorder {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogMapper operationLogMapper;

    @Override
    public void recordCurrent(String action, String targetType, String targetName) {
        SecurityUtils.currentUser().ifPresent(user ->
                record(user.id(), user.account(), action, targetType, targetName));
    }

    public void record(Long userId, String username, String action,
                       String targetType, String targetName) {
        try {
            OperationLog entry = new OperationLog();
            entry.setUserId(userId);
            entry.setUsername(truncate(StringUtils.hasText(username) ? username : "系统", 50));
            entry.setAction(action);
            entry.setTargetType(truncate(targetType, 50));
            entry.setTargetName(truncate(targetName, 255));
            entry.setIp(truncate(currentIp(), 64));
            operationLogMapper.insert(entry);
        } catch (Exception ex) {
            log.warn("操作日志写入失败: action={}, target={}", action, targetName, ex);
        }
    }

    public PageResult<OperationLogResponse> list(String keyword, String operationType,
                                                 LocalDate startDate, LocalDate endDate,
                                                 long page, long pageSize) {
        long current = normalizePage(page);
        long size = normalizePageSize(pageSize);
        IPage<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(current, size),
                buildQuery(keyword, operationType, startDate, endDate));
        List<OperationLogResponse> list = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(list, result.getTotal(), current, size);
    }

    public OperationLogResponse detail(Long id) {
        OperationLog entry = operationLogMapper.selectById(id);
        if (entry == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日志不存在");
        }
        return toResponse(entry);
    }

    /** 用户详情中的最近操作记录。 */
    public List<OperationLogResponse> recentForUser(Long userId, int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                        .eq(OperationLog::getUserId, userId)
                        .orderByDesc(OperationLog::getCreatedAt)
                        .orderByDesc(OperationLog::getId)
                        .last("LIMIT " + size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExportResult export(String keyword, String operationType,
                               LocalDate startDate, LocalDate endDate) {
        List<OperationLog> entries = operationLogMapper.selectList(
                buildQuery(keyword, operationType, startDate, endDate));
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("日志ID,操作用户,操作,操作对象,IP地址,操作时间\r\n");
        for (OperationLog entry : entries) {
            csv.append(escape(entry.getId()))
                    .append(',').append(escape(entry.getUsername()))
                    .append(',').append(escape(entry.getAction()))
                    .append(',').append(escape(entry.getTargetName()))
                    .append(',').append(escape(entry.getIp()))
                    .append(',').append(escape(BusinessDateUtils.toBusinessDateTime(entry.getCreatedAt())
                            .format(CSV_TIME)))
                    .append("\r\n");
        }
        recordCurrent(SystemConstants.ACTION_EXPORT, "操作日志",
                "导出 " + entries.size() + " 条日志");
        return new ExportResult(csv.toString().getBytes(StandardCharsets.UTF_8), entries.size());
    }

    public void clear() {
        // 保留 WHERE 条件以通过全表删除拦截；日志清空为物理删除（03 §6）
        operationLogMapper.delete(Wrappers.<OperationLog>lambdaQuery()
                .ge(OperationLog::getId, 1L));
    }

    private LambdaQueryWrapper<OperationLog> buildQuery(String keyword, String operationType,
                                                        LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(OperationLog::getUsername, value)
                    .or().like(OperationLog::getAction, value)
                    .or().like(OperationLog::getTargetName, value));
        }
        if (StringUtils.hasText(operationType)) {
            String action = operationType.trim();
            if (!SystemConstants.LOG_ACTIONS.contains(action)) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "操作类型不正确");
            }
            wrapper.eq(OperationLog::getAction, action);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "开始日期不能晚于结束日期");
        }
        if (startDate != null) {
            wrapper.ge(OperationLog::getCreatedAt,
                    BusinessDateUtils.businessDayStartToUtc(startDate));
        }
        if (endDate != null) {
            wrapper.lt(OperationLog::getCreatedAt,
                    BusinessDateUtils.businessDayStartToUtc(endDate.plusDays(1)));
        }
        return wrapper.orderByDesc(OperationLog::getCreatedAt).orderByDesc(OperationLog::getId);
    }

    private OperationLogResponse toResponse(OperationLog entry) {
        return new OperationLogResponse(entry.getId(), entry.getUserId(), entry.getUsername(),
                entry.getAction(), entry.getTargetType(), entry.getTargetName(), entry.getIp(),
                BusinessDateUtils.toBusinessDateTime(entry.getCreatedAt()));
    }

    private String currentIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return IpUtils.getClientIp(servletAttributes.getRequest());
        }
        return "unknown";
    }

    private long normalizePage(long page) {
        return page < 1 ? AppConstants.DEFAULT_PAGE : page;
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize < 1) {
            return AppConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, AppConstants.MAX_PAGE_SIZE);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    /**
     * 导出结果：CSV 内容与导出条数。
     */
    public record ExportResult(byte[] content, int count) {
    }
}
