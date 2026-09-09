package com.example.food.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AgentConversationMapper extends BaseMapper<AgentConversation> {

    @Select("SELECT * FROM agent_conversations WHERE id = #{conversationId} AND user_id = #{userId}")
    AgentConversation findOwned(@Param("userId") Long userId, @Param("conversationId") Long conversationId);

    @Select("SELECT * FROM agent_conversations WHERE user_id = #{userId} ORDER BY updated_at DESC, id DESC LIMIT 1")
    AgentConversation findLatest(@Param("userId") Long userId);

    @Update("UPDATE agent_conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = #{conversationId} AND user_id = #{userId}")
    int touch(@Param("userId") Long userId, @Param("conversationId") Long conversationId);

    @Delete("DELETE FROM agent_conversations WHERE id = #{conversationId} AND user_id = #{userId}")
    int deleteOwned(@Param("userId") Long userId, @Param("conversationId") Long conversationId);

    @Select("SELECT * FROM agent_conversations WHERE user_id = #{userId} ORDER BY updated_at DESC, id DESC")
    List<AgentConversation> listByUser(@Param("userId") Long userId);
}
