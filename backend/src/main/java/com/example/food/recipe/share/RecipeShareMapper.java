package com.example.food.recipe.share;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RecipeShareMapper extends BaseMapper<RecipeShare> {

    @Select("""
            SELECT *
            FROM recipe_shares
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<RecipeShare> findByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT *
            FROM recipe_shares
            WHERE id = #{shareId}
              AND user_id = #{userId}
            """)
    RecipeShare findOwned(
            @Param("userId") Long userId,
            @Param("shareId") Long shareId
    );

    @Select("""
            SELECT *
            FROM recipe_shares
            WHERE token = #{token}
            LIMIT 1
            """)
    RecipeShare findByToken(@Param("token") String token);

    @Update("""
            UPDATE recipe_shares
            SET disabled_at = #{disabledAt}
            WHERE id = #{shareId}
              AND user_id = #{userId}
              AND disabled_at IS NULL
            """)
    int disableOwned(
            @Param("userId") Long userId,
            @Param("shareId") Long shareId,
            @Param("disabledAt") java.time.LocalDateTime disabledAt
    );
}
