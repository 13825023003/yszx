package com.yunshu.zhixue.common.response;

import java.util.List;

/**
 * 统一分页响应（API 04 §2.4）：{@code {list, total, page, pageSize}}。
 *
 * <p>分页参数从 1 起，默认 pageSize=10，最大 100。</p>
 */
public record PageResult<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(List.of(), 0L, page, pageSize);
    }
}
