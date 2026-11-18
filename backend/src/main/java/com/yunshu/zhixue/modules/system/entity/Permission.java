package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunshu.zhixue.common.entity.TokenizedLogicDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限点（permission）：两层级，9 个组节点 + 34 个操作点（D4-5）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends TokenizedLogicDeleteEntity {

    /** 父节点 id；NULL 表示组节点。 */
    private Long parentId;

    /** 组名或权限点名称。 */
    private String name;

    /** 权限编码，如 {@code question:view}；组节点为该组英文名。 */
    private String code;

    /** 1 组（菜单级） / 2 操作点。 */
    private Integer type;

    private Integer sortOrder;
}
