package com.example.food.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    @Select({
            "<script>",
            "SELECT * FROM user_notifications",
            "WHERE user_id = #{userId}",
            "<if test='status != null'>AND status = #{status}</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<UserNotification> findPage(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM user_notifications",
            "WHERE user_id = #{userId}",
            "<if test='status != null'>AND status = #{status}</if>",
            "</script>"
    })
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM user_notifications WHERE user_id = #{userId} AND status = 'UNREAD'")
    long countUnread(@Param("userId") Long userId);

    @Select("SELECT * FROM user_notifications WHERE id = #{id} AND user_id = #{userId}")
    UserNotification findOwned(@Param("userId") Long userId, @Param("id") Long id);

    @Select("SELECT * FROM user_notifications WHERE user_id = #{userId} AND dedupe_key = #{dedupeKey}")
    UserNotification findByDedupeKey(@Param("userId") Long userId, @Param("dedupeKey") String dedupeKey);

    @Update("""
            UPDATE user_notifications
            SET status = 'READ', read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
            WHERE id = #{id} AND user_id = #{userId} AND status = 'UNREAD'
            """)
    int markRead(@Param("userId") Long userId, @Param("id") Long id);

    @Update("""
            UPDATE user_notifications
            SET status = 'READ', read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
            WHERE user_id = #{userId} AND status = 'UNREAD'
            """)
    int markAllRead(@Param("userId") Long userId);

    @Update("""
            UPDATE user_notifications
            SET status = 'ARCHIVED', archived_at = COALESCE(archived_at, CURRENT_TIMESTAMP)
            WHERE id = #{id} AND user_id = #{userId} AND status <> 'ARCHIVED'
            """)
    int archive(@Param("userId") Long userId, @Param("id") Long id);
}
