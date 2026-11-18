package com.yunshu.zhixue.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 双端 JWT 配置。密钥必须通过环境变量提供，不得写入代码仓库（02 §2.4）。
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @Valid
    @NotNull
    private Domain portal = new Domain();

    @Valid
    @NotNull
    private Domain admin = new Domain();

    public Domain forDomain(TokenDomain domain) {
        return domain == TokenDomain.ADMIN ? admin : portal;
    }

    @Data
    public static class Domain {

        /** HS256 密钥，长度至少 32 字节。 */
        @NotBlank
        private String secret;

        @NotNull
        private Duration accessTokenTtl = Duration.ofHours(2);

        @NotNull
        private Duration refreshTokenTtl = Duration.ofDays(14);

        @NotBlank
        private String issuer;
    }
}
