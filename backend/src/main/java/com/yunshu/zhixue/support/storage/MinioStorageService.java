package com.yunshu.zhixue.support.storage;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 MinIO（S3 兼容协议）的存储实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "image/svg+xml", ".svg");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public String upload(InputStream inputStream, long size, String originalFilename,
                         String contentType, String directory) {
        String objectName = normalizeDirectory(directory)
                + "/" + UUID.randomUUID().toString().replace("-", "")
                + resolveExtension(originalFilename, contentType);
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return buildPublicUrl(objectName);
        } catch (Exception ex) {
            log.error("对象上传失败: object={}", objectName, ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件上传失败", ex);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            // 删除失败不回滚业务，只记录日志
            log.warn("对象删除失败: object={}", objectName, ex);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(properties.getBucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            log.info("MinIO 桶不存在，已自动创建: {}", properties.getBucket());
        }
    }

    private String normalizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "common";
        }
        String normalized = directory.trim()
                .replaceAll("[^a-zA-Z0-9/_-]", "")
                .replaceAll("/{2,}", "/")
                .replaceAll("^/+|/+$", "");
        if (normalized.isBlank() || normalized.contains("..")) {
            return "common";
        }
        return normalized;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
                if (ext.length() <= 9 && ext.matches("\\.[a-z0-9]+")) {
                    return ext;
                }
            }
        }
        return CONTENT_TYPE_EXTENSIONS.getOrDefault(
                contentType == null ? "" : contentType.toLowerCase(Locale.ROOT), "");
    }

    private String buildPublicUrl(String objectName) {
        String base = properties.getPublicBasePath();
        String normalizedBase = base.endsWith("/")
                ? base.substring(0, base.length() - 1)
                : base;
        return normalizedBase + "/" + objectName;
    }
}
