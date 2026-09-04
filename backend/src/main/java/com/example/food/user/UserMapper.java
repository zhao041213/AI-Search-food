package com.example.food.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE phone = #{phone} FOR UPDATE")
    User selectByPhoneForUpdate(@Param("phone") String phone);

    @Select("SELECT * FROM users WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash},
                password_failed_attempts = #{passwordFailedAttempts},
                password_locked_until = #{passwordLockedUntil},
                auth_version = COALESCE(#{authVersion}, 0),
                last_login_at = #{lastLoginAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePasswordState(User user);

    @Update("""
            UPDATE users
            SET nickname = #{nickname},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND enabled = 1
              AND deleted_at IS NULL
            """)
    int updateNickname(User user);

    @Update("""
            UPDATE users
            SET avatar_url = #{avatarUrl},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND enabled = 1
              AND deleted_at IS NULL
            """)
    int updateAvatar(User user);

    @Update("""
            UPDATE users
            SET enabled = #{enabled},
                deleted_at = #{deletedAt},
                auth_version = #{authVersion},
                password_hash = #{passwordHash},
                password_failed_attempts = #{passwordFailedAttempts},
                password_locked_until = #{passwordLockedUntil},
                avatar_url = #{avatarUrl},
                phone = #{phone},
                nickname = #{nickname},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int cancelAccount(User user);

    @Select({
            "<script>",
            "SELECT * FROM users",
            "WHERE role = 'USER' AND deleted_at IS NULL",
            "<if test='keyword != null'>",
            "  AND (phone LIKE CONCAT('%', #{keyword}, '%')",
            "       OR nickname LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='enabled != null'>",
            "  AND enabled = #{enabled}",
            "</if>",
            "<if test='locked != null'>",
            "  <choose>",
            "    <when test='locked'>",
            "      AND password_locked_until IS NOT NULL AND password_locked_until &gt; #{now}",
            "    </when>",
            "    <otherwise>",
            "      AND (password_locked_until IS NULL OR password_locked_until &lt;= #{now})",
            "    </otherwise>",
            "  </choose>",
            "</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<User> findAdminPage(
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled,
            @Param("locked") Boolean locked,
            @Param("now") LocalDateTime now,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM users",
            "WHERE role = 'USER' AND deleted_at IS NULL",
            "<if test='keyword != null'>",
            "  AND (phone LIKE CONCAT('%', #{keyword}, '%')",
            "       OR nickname LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='enabled != null'>",
            "  AND enabled = #{enabled}",
            "</if>",
            "<if test='locked != null'>",
            "  <choose>",
            "    <when test='locked'>",
            "      AND password_locked_until IS NOT NULL AND password_locked_until &gt; #{now}",
            "    </when>",
            "    <otherwise>",
            "      AND (password_locked_until IS NULL OR password_locked_until &lt;= #{now})",
            "    </otherwise>",
            "  </choose>",
            "</if>",
            "</script>"
    })
    long countAdminUsers(
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled,
            @Param("locked") Boolean locked,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE users
            SET enabled = #{enabled},
                auth_version = #{authVersion},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND role = 'USER'
              AND deleted_at IS NULL
            """)
    int updateAdminStatus(User user);

    @Update("""
            UPDATE users
            SET password_failed_attempts = 0,
                password_locked_until = NULL,
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND role = 'USER'
              AND deleted_at IS NULL
            """)
    int clearPasswordLock(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE users
            SET auth_version = auth_version + 1,
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND enabled = 1
              AND deleted_at IS NULL
            """)
    int incrementAuthVersion(@Param("id") Long id, @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
