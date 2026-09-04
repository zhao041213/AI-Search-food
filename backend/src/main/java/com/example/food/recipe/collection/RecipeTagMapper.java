package com.example.food.recipe.collection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeTagMapper extends BaseMapper<RecipeTag> {

    @Select("""
            SELECT *
            FROM recipe_tags
            WHERE user_id = #{userId}
              AND name = #{name}
            LIMIT 1
            """)
    RecipeTag findByUserIdAndName(
            @Param("userId") Long userId,
            @Param("name") String name
    );

    @Select("SELECT COUNT(*) FROM recipe_tags WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT rt.id, rt.name, COUNT(rrt.id) AS recipe_count
            FROM recipe_tags rt
            LEFT JOIN recipe_record_tags rrt
              ON rrt.tag_id = rt.id
             AND rrt.user_id = #{userId}
            WHERE rt.user_id = #{userId}
            GROUP BY rt.id, rt.name
            ORDER BY rt.name ASC, rt.id ASC
            """)
    List<RecipeTagSummaryRow> findSummariesByUserId(@Param("userId") Long userId);
}
