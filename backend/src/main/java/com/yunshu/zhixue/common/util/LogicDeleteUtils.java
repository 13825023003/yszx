package com.yunshu.zhixue.common.util;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

/**
 * 逻辑删除工具：保证 {@code delete_flag=1} 与 {@code deleted_token=id} 同时写入（R4）。
 *
 * <p>用法：{@code mapper.update(null, LogicDeleteUtils.markDeleted(id))}。
 * 含 {@code deleted_token} 的表必须使用本工具，普通逻辑删除表可继续使用
 * {@code removeById}。</p>
 */
public final class LogicDeleteUtils {

    private LogicDeleteUtils() {
    }

    public static <T> UpdateWrapper<T> markDeleted(long id) {
        return new UpdateWrapper<T>()
                .eq("id", id)
                .eq("delete_flag", 0)
                .set("delete_flag", 1)
                .setSql("deleted_token = id");
    }
}
