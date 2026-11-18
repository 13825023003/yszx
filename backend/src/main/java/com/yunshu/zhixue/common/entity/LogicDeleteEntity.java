package com.yunshu.zhixue.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带逻辑删除的实体基类（全站逻辑删除，D14）。
 *
 * <p>对应表中仅有 {@code delete_flag}、没有 {@code deleted_token} 的场景；
 * 含业务唯一键与 {@code deleted_token} 的表请继承 {@link TokenizedLogicDeleteEntity}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class LogicDeleteEntity extends BaseEntity {

    @TableLogic(value = "0", delval = "1")
    @TableField("delete_flag")
    private Integer deleteFlag;
}
