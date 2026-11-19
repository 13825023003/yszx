package com.yunshu.zhixue.modules.interviewbank.mapper;

import com.yunshu.zhixue.support.query.CountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 面试题库对外统计：按学科统计未删除面试题数。
 */
@Mapper
public interface InterviewQuestionUsageMapper {

    @Select("""
            <script>
            SELECT subject_id AS id, COUNT(*) AS total
            FROM interview_question
            WHERE delete_flag = 0
              AND subject_id IN
              <foreach collection="subjectIds" item="subjectId" open="(" separator="," close=")">
                #{subjectId}
              </foreach>
            GROUP BY subject_id
            </script>
            """)
    List<CountRow> countBySubjectIds(@Param("subjectIds") Collection<Long> subjectIds);
}
