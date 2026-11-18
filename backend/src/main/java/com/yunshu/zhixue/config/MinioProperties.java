package com.yunshu.zhixue.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 对象存储配置（S3 兼容协议，本期自建 MinIO，D8/D24）。
 *
 * <p>连接信息全部来自环境变量，不落库、不写入前端代码。</p>
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class MinioProperties {

    /** MinIO S3 API 地址，如 http://localhost:9000。 */
    @NotBlank
    private String endpoint;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    /** 业务桶名，由 deploy 的 minio-init 自动创建。 */
    @NotBlank
    private String bucket;

    /** 公开访问前缀，Nginx 将 /files 反代至 MinIO 桶（D20）。 */
    @NotBlank
    private String publicBasePath = "/files";
}
