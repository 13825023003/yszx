package com.yunshu.zhixue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 契约配置（D21）：{@code /v3/api-docs} 是两端类型生成的唯一契约源。
 *
 * <p>两端令牌域独立，Swagger 中分别提供 portalBearer / adminBearer 两种认证方式。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI yunshuOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("云枢智学 API")
                        .version("v1")
                        .description("学员端前缀 /api/portal/v1，管理端前缀 /api/admin/v1。"
                                + "统一响应结构、分页与错误码见 docs/design/04.API接口设计.md。"))
                .components(new Components()
                        .addSecuritySchemes("portalBearer", bearerScheme("学员端 JWT"))
                        .addSecuritySchemes("adminBearer", bearerScheme("管理端 JWT")));
    }

    private SecurityScheme bearerScheme(String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(description);
    }
}
