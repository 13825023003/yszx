package com.yunshu.zhixue.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM course WHERE delete_flag = 0")
    long selectTotalDurationMinutes();

    @Select("""
            SELECT COUNT(DISTINCT t.sid)
            FROM (
                SELECT c.subject_id AS sid
                FROM course c
                WHERE c.delete_flag = 0 AND c.subject_id IS NOT NULL
                UNION
                SELECT cs.subject_id
                FROM course_subject cs
                JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0
                WHERE cs.delete_flag = 0
            ) t
            """)
    long countAssociatedSubjects();

    @Select("""
            <script>
            SELECT bvid FROM course
            WHERE delete_flag = 0 AND bvid IS NOT NULL
              AND bvid IN
              <foreach collection="bvidList" item="bvid" open="(" separator="," close=")">
                #{bvid}
              </foreach>
            </script>
            """)
    List<String> selectExistingBvids(@Param("bvidList") Collection<String> bvidList);
}
