package com.example.food.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentConfirmationMapper extends BaseMapper<AgentConfirmation> {

    @Select("SELECT * FROM agent_confirmations WHERE id = #{confirmationId} AND user_id = #{userId}")
    AgentConfirmation findOwned(@Param("userId") Long userId, @Param("confirmationId") Long confirmationId);

    @Update("UPDATE agent_confirmations SET status = 'PROCESSING' WHERE id = #{confirmationId} AND user_id = #{userId} AND status = 'PENDING'")
    int claim(@Param("userId") Long userId, @Param("confirmationId") Long confirmationId);

    @Update("UPDATE agent_confirmations SET status = 'CONFIRMED', confirmed_at = CURRENT_TIMESTAMP WHERE id = #{confirmationId} AND user_id = #{userId} AND status = 'PROCESSING'")
    int markConfirmed(@Param("userId") Long userId, @Param("confirmationId") Long confirmationId);
}
