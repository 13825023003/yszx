package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（operation_log）：审计表，无逻辑删除，清空为物理删除。
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 用户名快照，账号改名后仍保留操作时名称。 */
    private String username;

    /** 登录/登出/新增/修改/删除/禁用启用/导出/其他。 */
    private String action;

    private String targetType;

    private String targetName;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
