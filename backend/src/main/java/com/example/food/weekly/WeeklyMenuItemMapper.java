package com.example.food.weekly;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WeeklyMenuItemMapper extends BaseMapper<WeeklyMenuItem> {

    @Select("""
            SELECT *
            FROM weekly_menu_items
            WHERE plan_id = #{planId}
            ORDER BY menu_date ASC,
                CASE meal_type
                    WHEN 'BREAKFAST' THEN 1
                    WHEN 'LUNCH' THEN 2
                    WHEN 'DINNER' THEN 3
                    ELSE 4
                END,
                id ASC
            """)
    List<WeeklyMenuItem> findByPlanId(@Param("planId") Long planId);

    @Delete("DELETE FROM weekly_menu_items WHERE plan_id = #{planId}")
    int deleteByPlanId(@Param("planId") Long planId);
}
