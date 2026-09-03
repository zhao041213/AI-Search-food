package com.example.food.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE phone = #{phone} FOR UPDATE")
    User selectByPhoneForUpdate(@Param("phone") String phone);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash},
                password_failed_attempts = #{passwordFailedAttempts},
                password_locked_until = #{passwordLockedUntil},
                last_login_at = #{lastLoginAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePasswordState(User user);
}
