package com.yunshu.zhixue.security;

import java.util.Arrays;

/**
 * 令牌域：学员端与管理端完全独立（D12/D7），令牌互不通用。
 */
public enum TokenDomain {

    PORTAL("portal"),
    ADMIN("admin");

    private final String code;

    TokenDomain(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TokenDomain fromCode(String code) {
        return Arrays.stream(values())
                .filter(domain -> domain.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知令牌域: " + code));
    }
}
