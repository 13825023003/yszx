package com.yunshu.zhixue.modules.question.mapper;

import com.yunshu.zhixue.support.query.CountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 题模块对外统计：只访问 question 表，供学科/阶段删除保护与统计使用。
 */
@Mapper
public interface QuestionUsageMapper {

    @Select("SELECT COUNT(*) FROM question WHERE delete_flag = 0")
    long countAll();

    @Select("""
            <script>
            SELECT stage_id AS id, COUNT(*) AS total
            FROM question
            WHERE delete_flag = 0
              AND stage_id IN
              <foreach collection="stageIds" item="stageId" open="(" separator="," close=")">
                #{stageId}
              </foreach>
            GROUP BY stage_id
            </script>
            """)
    List<CountRow> countByStageIds(@Param("stageIds") Collection<Long> stageIds);
}
