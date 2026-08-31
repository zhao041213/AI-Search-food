package com.example.food.weekly;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WeeklyMenuPlanMapper extends BaseMapper<WeeklyMenuPlan> {

    @Select("""
            SELECT *
            FROM weekly_menu_plans
            WHERE user_id = #{userId}
              AND week_start_date = #{weekStartDate}
            """)
    WeeklyMenuPlan findByUserIdAndWeekStart(
            @Param("userId") Long userId,
            @Param("weekStartDate") java.time.LocalDate weekStartDate
    );
}
