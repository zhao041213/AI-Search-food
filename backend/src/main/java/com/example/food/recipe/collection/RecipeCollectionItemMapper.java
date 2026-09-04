package com.example.food.recipe.collection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RecipeCollectionItemMapper extends BaseMapper<RecipeCollectionItem> {

    @Select("""
            SELECT *
            FROM recipe_collection_items
            WHERE user_id = #{userId}
              AND recipe_id = #{recipeId}
            LIMIT 1
            """)
    RecipeCollectionItem findByUserIdAndRecipeId(
            @Param("userId") Long userId,
            @Param("recipeId") Long recipeId
    );

    @Update("""
            UPDATE recipe_collection_items
            SET collection_id = #{collectionId}
            WHERE user_id = #{userId}
              AND recipe_id = #{recipeId}
            """)
    int moveOwnedRecipe(
            @Param("userId") Long userId,
            @Param("recipeId") Long recipeId,
            @Param("collectionId") Long collectionId
    );

    @Update("""
            UPDATE recipe_collection_items
            SET collection_id = #{defaultCollectionId}
            WHERE user_id = #{userId}
              AND collection_id = #{sourceCollectionId}
            """)
    int moveAllToDefault(
            @Param("userId") Long userId,
            @Param("sourceCollectionId") Long sourceCollectionId,
            @Param("defaultCollectionId") Long defaultCollectionId
    );
}
