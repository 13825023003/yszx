package com.yunshu.zhixue.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 审计字段自动填充：createdAt / updatedAt 按 UTC 写入（database-rules §3.5）。
 *
 * <p>deletedToken 在插入时默认写 0；逻辑删除置为 id 由
 * {@link com.yunshu.zhixue.common.util.LogicDeleteUtils} 负责。</p>
 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "deletedToken", Long.class, 0L);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class,
                LocalDateTime.now(ZoneOffset.UTC));
    }
}
