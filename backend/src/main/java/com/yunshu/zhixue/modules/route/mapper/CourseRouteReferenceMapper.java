package com.yunshu.zhixue.modules.route.mapper;

import com.yunshu.zhixue.support.query.CountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 课程被未删除学习路线引用的数量。
 */
@Mapper
public interface CourseRouteReferenceMapper {

    @Select("""
            <script>
            SELECT rc.course_id AS id, COUNT(DISTINCT rc.route_id) AS total
            FROM route_course rc
            JOIN learning_route r ON r.id = rc.route_id AND r.delete_flag = 0
            WHERE rc.delete_flag = 0
              AND rc.course_id IN
              <foreach collection="courseIds" item="courseId" open="(" separator="," close=")">
                #{courseId}
              </foreach>
            GROUP BY rc.course_id
            </script>
            """)
    List<CountRow> countByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
