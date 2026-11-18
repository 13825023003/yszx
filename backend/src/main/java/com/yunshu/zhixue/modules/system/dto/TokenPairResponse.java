package com.yunshu.zhixue.modules.system.dto;

import com.yunshu.zhixue.security.TokenPair;

/**
 * 双令牌响应：只暴露接口契约字段，不暴露 jti 等内部结构。
 */
public record TokenPairResponse(String accessToken,
                                String refreshToken,
                                long accessExpiresIn,
                                long refreshExpiresIn) {

    public static TokenPairResponse from(TokenPair pair) {
        return new TokenPairResponse(pair.accessToken(), pair.refreshToken(),
                pair.accessExpiresIn(), pair.refreshExpiresIn());
    }
}
