package com.example.food.suggestion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeatureSuggestionMapper extends BaseMapper<FeatureSuggestion> {
    @Select("SELECT * FROM feature_suggestions WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL")
    FeatureSuggestion findOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM feature_suggestions WHERE id = #{id} AND deleted_at IS NULL")
    FeatureSuggestion findActive(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM feature_suggestions WHERE user_id = #{userId} AND created_at >= #{since} AND deleted_at IS NULL")
    long countRecent(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Select("SELECT * FROM feature_suggestions WHERE user_id = #{userId} AND LOWER(title) = LOWER(#{title}) AND created_at >= #{since} AND deleted_at IS NULL ORDER BY id DESC LIMIT 1")
    FeatureSuggestion findRecentTitle(@Param("userId") Long userId, @Param("title") String title, @Param("since") LocalDateTime since);

    @Select({"<script>", "SELECT * FROM feature_suggestions WHERE user_id = #{userId} AND deleted_at IS NULL", "ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<FeatureSuggestion> findUserPage(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM feature_suggestions WHERE user_id = #{userId} AND deleted_at IS NULL")
    long countUser(@Param("userId") Long userId);

    @Select({"<script>", "SELECT * FROM feature_suggestions WHERE deleted_at IS NULL",
            "<if test='keyword != null'>AND (title LIKE CONCAT('%', #{keyword}, '%') OR detail LIKE CONCAT('%', #{keyword}, '%'))</if>",
            "<if test='type != null'>AND type = #{type}</if>", "<if test='status != null'>AND status = #{status}</if>",
            "ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<FeatureSuggestion> findAdminPage(@Param("keyword") String keyword, @Param("type") String type,
                                           @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM feature_suggestions WHERE deleted_at IS NULL",
            "<if test='keyword != null'>AND (title LIKE CONCAT('%', #{keyword}, '%') OR detail LIKE CONCAT('%', #{keyword}, '%'))</if>",
            "<if test='type != null'>AND type = #{type}</if>", "<if test='status != null'>AND status = #{status}</if>", "</script>"})
    long countAdmin(@Param("keyword") String keyword, @Param("type") String type, @Param("status") String status);

    @Update("""
            UPDATE feature_suggestions SET status = #{item.status}, admin_reply = #{item.adminReply},
                internal_note = #{item.internalNote}, duplicate_of_id = #{item.duplicateOfId},
                updated_at = #{item.updatedAt}, last_processed_at = #{item.lastProcessedAt}
            WHERE id = #{item.id} AND deleted_at IS NULL
            """)
    int updateAdmin(@Param("item") FeatureSuggestion item);

    @Update("UPDATE feature_suggestions SET deleted_at = #{deletedAt}, updated_at = #{deletedAt} WHERE id = #{id} AND deleted_at IS NULL")
    int logicalDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
