package com.yunshu.zhixue.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 能力层配置（OpenAI 兼容协议，D4/D15）。
 *
 * <p>base-url / api-key / model 全部来自环境变量，不落库。
 * 具体模型客户端、提示词模板与降级策略在 ai 模块实施阶段实现。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String baseUrl;

    private String apiKey;

    private String model;

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(30);

    /** 简答批改同步调用的超时，超时后降级为关键词规则判分（05 评审 §4.2）。 */
    private Duration gradingTimeout = Duration.ofSeconds(10);

    private int maxRetries = 2;
}
