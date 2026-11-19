package com.yunshu.zhixue.support.query;

import lombok.Data;

/**
 * 通用分组计数行：SQL 将业务 id 起别名为 {@code id}。
 */
@Data
public class CountRow {

    private Long id;

    private Long total;
}
