package com.example.food.shopping;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShoppingItemCheckMapper extends BaseMapper<ShoppingItemCheck> {

    @Select("""
            SELECT *
            FROM shopping_item_checks
            WHERE user_id = #{userId}
              AND search_log_id = #{searchLogId}
            ORDER BY id ASC
            """)
    List<ShoppingItemCheck> findByUserIdAndSearchLogId(
            @Param("userId") Long userId,
            @Param("searchLogId") Long searchLogId
    );

    @Select("""
            SELECT *
            FROM shopping_item_checks
            WHERE user_id = #{userId}
              AND search_log_id = #{searchLogId}
              AND ingredient_name = #{ingredientName}
            """)
    ShoppingItemCheck findByIdentity(
            @Param("userId") Long userId,
            @Param("searchLogId") Long searchLogId,
            @Param("ingredientName") String ingredientName
    );
}
