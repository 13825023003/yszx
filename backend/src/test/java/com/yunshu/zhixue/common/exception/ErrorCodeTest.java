package com.yunshu.zhixue.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void businessCodesAreUnique() {
        long distinct = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();
        assertThat(distinct).isEqualTo(ErrorCode.values().length);
    }

    @Test
    void businessCodesFollowSegmentRule() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            int code = errorCode.getCode();
            assertThat(code).isBetween(10000, 39999);
            int segment = code / 10000;
            assertThat(segment).isIn(1, 2, 3);
        }
    }
}
