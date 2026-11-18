package com.yunshu.zhixue;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 云枢智学后端启动类。
 *
 * <p>架构形态：模块化单体。业务代码统一落在 {@code com.yunshu.zhixue.modules.<领域>}，
 * 模块间单向依赖、只经 Service 接口与 DTO 交互（详见 docs/design/02.整体架构设计.md §3.2）。</p>
 */
@SpringBootApplication
@MapperScan(basePackages = "com.yunshu.zhixue.modules", annotationClass = Mapper.class)
public class YunshuZhixueApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunshuZhixueApplication.class, args);
    }
}
