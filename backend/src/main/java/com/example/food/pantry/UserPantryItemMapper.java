package com.example.food.pantry;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
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

    @Select("""
            SELECT *
            FROM user_pantry_items
            WHERE user_id = #{userId}
              AND quantity IS NOT NULL
              AND quantity > 0
              AND expire_date IS NOT NULL
              AND expire_date <= #{warningUntil}
            ORDER BY expire_date ASC, updated_at DESC, id DESC
            """)
    List<UserPantryItem> findExpiryAlertsByUserId(
            @Param("userId") Long userId,
            @Param("warningUntil") LocalDate warningUntil
    );

    @Update("""
            UPDATE user_pantry_items
            SET quantity = quantity - #{quantity}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{itemId}
              AND user_id = #{userId}
              AND quantity IS NOT NULL
              AND quantity >= #{quantity}
            """)
    int consumeByUserIdAndId(
            @Param("userId") Long userId,
            @Param("itemId") Long itemId,
            @Param("quantity") java.math.BigDecimal quantity
    );

    @Select("""
            SELECT * FROM user_pantry_items
            WHERE user_id = #{userId}
              AND quantity IS NOT NULL AND quantity > 0
            ORDER BY CASE WHEN expire_date IS NULL THEN 1 ELSE 0 END, expire_date ASC, id ASC
            FOR UPDATE
    """)
    /** Locks all positive batches for the user; the service applies alias normalization before allocation. */
    List<UserPantryItem> findAvailableForUpdate(@Param("userId") Long userId, @Param("ingredientName") String ingredientName);

    @Select("SELECT * FROM user_pantry_items WHERE id = #{id} AND user_id = #{userId} FOR UPDATE")
    UserPantryItem findOwnedForUpdate(@Param("userId") Long userId, @Param("id") Long id);
}
