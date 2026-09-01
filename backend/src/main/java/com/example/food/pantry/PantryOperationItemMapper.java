package com.example.food.pantry;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PantryOperationItemMapper extends BaseMapper<PantryOperationItem> {
    @Select("SELECT * FROM pantry_operation_items WHERE operation_id = #{operationId} ORDER BY id ASC")
    List<PantryOperationItem> findByOperationId(@Param("operationId") Long operationId);

    @Select("""
            SELECT COUNT(*) FROM pantry_operation_items poi
            JOIN pantry_operations po ON po.id = poi.operation_id
            WHERE po.user_id = #{userId}
              AND poi.pantry_item_id = #{pantryItemId}
              AND po.status = 'COMPLETED'
              AND (po.created_at > #{createdAt}
                   OR (po.created_at = #{createdAt} AND po.id > #{operationId}))
            """)
    int countLaterOperations(@Param("userId") Long userId, @Param("pantryItemId") Long pantryItemId,
                             @Param("createdAt") LocalDateTime createdAt, @Param("operationId") Long operationId);
}
