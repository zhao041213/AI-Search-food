package com.example.food.weekly;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WeeklyMenuShoppingCheckMapper extends BaseMapper<WeeklyMenuShoppingCheck> {

    @Select("""
            SELECT *
            FROM weekly_menu_shopping_checks
            WHERE user_id = #{userId}
              AND plan_id = #{planId}
            ORDER BY id ASC
            """)
    List<WeeklyMenuShoppingCheck> findByUserIdAndPlanId(
            @Param("userId") Long userId,
            @Param("planId") Long planId
    );

    @Select("""
            SELECT *
            FROM weekly_menu_shopping_checks
            WHERE user_id = #{userId}
              AND plan_id = #{planId}
              AND ingredient_name = #{ingredientName}
            """)
    WeeklyMenuShoppingCheck findByIdentity(
            @Param("userId") Long userId,
            @Param("planId") Long planId,
            @Param("ingredientName") String ingredientName
    );
}
