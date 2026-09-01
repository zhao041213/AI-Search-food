package com.example.food.pantry;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PantryOperationMapper extends BaseMapper<PantryOperation> {
    @Select("SELECT * FROM pantry_operations WHERE user_id = #{userId} AND operation_type = #{type} AND idempotency_key = #{key} LIMIT 1")
    PantryOperation findByIdempotency(@Param("userId") Long userId, @Param("type") String type, @Param("key") String key);

    @Select("SELECT * FROM pantry_operations WHERE user_id = #{userId} ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<PantryOperation> findRecent(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE pantry_operations SET status = #{status}, reversed_operation_id = #{reversedId}, reversed_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId} AND status = 'COMPLETED'")
    int markReversed(@Param("id") Long id, @Param("userId") Long userId, @Param("status") String status, @Param("reversedId") Long reversedId);
}
