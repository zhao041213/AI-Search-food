package com.example.food.recipe.collection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeRecordTagMapper extends BaseMapper<RecipeRecordTag> {

    @Select("""
            <script>
            SELECT rrt.recipe_id, rt.id AS tag_id, rt.name AS tag_name
            FROM recipe_record_tags rrt
            INNER JOIN recipe_tags rt
              ON rt.id = rrt.tag_id
             AND rt.user_id = #{userId}
            WHERE rrt.user_id = #{userId}
              AND rrt.recipe_id IN
              <foreach collection="recipeIds" item="recipeId" open="(" separator="," close=")">
                #{recipeId}
              </foreach>
            ORDER BY rrt.recipe_id ASC, rt.name ASC, rt.id ASC
            </script>
            """)
    List<SavedRecipeTagRow> findRowsByUserIdAndRecipeIds(
            @Param("userId") Long userId,
            @Param("recipeIds") List<Long> recipeIds
    );

    @Select("""
            SELECT *
            FROM recipe_record_tags
            WHERE user_id = #{userId}
              AND recipe_id = #{recipeId}
            ORDER BY id ASC
            """)
    List<RecipeRecordTag> findByUserIdAndRecipeId(
            @Param("userId") Long userId,
            @Param("recipeId") Long recipeId
    );

    @Delete("""
            DELETE FROM recipe_record_tags
            WHERE user_id = #{userId}
              AND recipe_id = #{recipeId}
            """)
    int deleteByUserIdAndRecipeId(
            @Param("userId") Long userId,
            @Param("recipeId") Long recipeId
    );

    @Delete("""
            DELETE FROM recipe_record_tags
            WHERE user_id = #{userId}
              AND recipe_id = #{recipeId}
              AND tag_id = #{tagId}
            """)
    int deleteByUserIdAndRecipeIdAndTagId(
            @Param("userId") Long userId,
            @Param("recipeId") Long recipeId,
            @Param("tagId") Long tagId
    );
}
