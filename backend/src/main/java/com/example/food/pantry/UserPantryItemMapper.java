package com.example.food.pantry;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPantryItemMapper extends BaseMapper<UserPantryItem> {

    @Select("""
            SELECT *
            FROM user_pantry_items
            WHERE user_id = #{userId}
            ORDER BY
                CASE WHEN expire_date IS NULL THEN 1 ELSE 0 END,
                expire_date ASC,
                updated_at DESC,
                id DESC
            """)
    List<UserPantryItem> findByUserId(@Param("userId") Long userId);
}
