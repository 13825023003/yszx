package com.yunshu.zhixue.support.storage;

import java.io.InputStream;

/**
 * 对象存储抽象（D8）：业务模块只依赖本接口，切换 S3 兼容存储时无需改动业务代码。
 */
public interface StorageService {

    /**
     * 上传对象。
     *
     * @param inputStream      文件流
     * @param size             文件大小（字节）
     * @param originalFilename 原始文件名（用于推断扩展名）
     * @param contentType      MIME 类型
     * @param directory        逻辑目录，如 {@code avatar}、{@code logo}
     * @return 公开访问路径，如 {@code /files/avatar/xxx.png}
     */
    String upload(InputStream inputStream, long size, String originalFilename,
                  String contentType, String directory);

    /**
     * 删除对象。
     *
     * @param objectName 对象名（上传时返回路径去掉公开前缀后的部分）
     */
    void delete(String objectName);
}
