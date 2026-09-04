package com.example.food.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserNotificationPreferenceMapper extends BaseMapper<UserNotificationPreference> {

    @Select("SELECT * FROM user_notification_preferences WHERE user_id = #{userId}")
    UserNotificationPreference findByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE user_notification_preferences
            SET pantry_expiring_enabled = #{pantryExpiringEnabled},
                pantry_expired_enabled = #{pantryExpiredEnabled},
                weekly_menu_preparation_enabled = #{weeklyMenuPreparationEnabled},
                updated_at = #{updatedAt}
            WHERE user_id = #{userId}
            """)
    int updateByUserId(UserNotificationPreference preference);
}
