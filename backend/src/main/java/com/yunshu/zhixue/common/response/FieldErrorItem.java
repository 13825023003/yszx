package com.yunshu.zhixue.common.response;

/**
 * 参数校验失败时的字段级错误（API 04 §2.3）。
 *
 * @param field  字段名
 * @param reason 面向用户的原因
 */
public record FieldErrorItem(String field, String reason) {
}
