package com.yunshu.zhixue.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带 {@code deleted_token} 的逻辑删除实体基类（R4 决议）。
 *
 * <p>唯一索引形如 {@code (业务键..., deleted_token)}：正常行 {@code deleted_token=0}，
 * 逻辑删除时必须在同一事务内同时写 {@code delete_flag=1} 与 {@code deleted_token=id}。
 * 请使用 {@link com.yunshu.zhixue.common.util.LogicDeleteUtils#markDeleted(long)} 完成删除，
 * 不要直接调用 MyBatis-Plus 的 removeById，否则唯一键无法释放。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TokenizedLogicDeleteEntity extends LogicDeleteEntity {

    @TableField(fill = FieldFill.INSERT)
    private Long deletedToken;
}
