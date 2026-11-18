package com.yunshu.zhixue.common.response;

/**
 * 批量导入校验失败时的行级错误（业务码 30005）。
 *
 * @param row    行号（以导入文件为准）
 * @param field  字段名
 * @param reason 面向用户的原因
 */
public record RowErrorItem(Integer row, String field, String reason) {
}
