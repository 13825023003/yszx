package com.yunshu.zhixue.security;

/**
 * 一次签发得到的双令牌及其有效期（秒）。
 *
 * <p>接口出参由 auth 模块组装为响应 DTO，不直接暴露本内部结构。</p>
 */
public record TokenPair(String accessToken,
                        String refreshToken,
                        long accessExpiresIn,
                        long refreshExpiresIn,
                        String accessJti,
                        String refreshJti) {
}
