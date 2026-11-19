package com.yunshu.zhixue.modules.course.mapper;

import com.yunshu.zhixue.support.query.CountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 课程模块对外统计：主学科 + 额外学科去重计数。
 */
@Mapper
public interface CourseUsageMapper {

    @Select("""
            <script>
            SELECT t.sid AS id, COUNT(DISTINCT t.course_id) AS total
            FROM (
                SELECT c.id AS course_id, c.subject_id AS sid
                FROM course c
                WHERE c.delete_flag = 0
                  AND c.subject_id IN
                  <foreach collection="subjectIds" item="subjectId" open="(" separator="," close=")">
                    #{subjectId}
                  </foreach>
                UNION ALL
                SELECT cs.course_id, cs.subject_id
                FROM course_subject cs
                JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0
                WHERE cs.delete_flag = 0
                  AND cs.subject_id IN
                  <foreach collection="subjectIds" item="subjectId" open="(" separator="," close=")">
                    #{subjectId}
                  </foreach>
            ) t
            GROUP BY t.sid
            </script>
            """)
    List<CountRow> countBySubjectIds(@Param("subjectIds") Collection<Long> subjectIds);
}
