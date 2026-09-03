package com.example.food.admin.error;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminErrorLogMapper extends BaseMapper<AdminErrorLog> {

    @Select({
            "<script>",
            "SELECT id, source_type, severity, component, exception_class, message, root_cause,",
            "request_method, request_path, status_code, user_id, admin_id, ip_address,",
            "NULL AS stack_trace, created_at FROM system_error_logs",
            "WHERE (#{sourceType} IS NULL OR source_type = #{sourceType})",
            "AND (#{keyword} IS NULL OR source_type LIKE CONCAT('%', #{keyword}, '%')",
            "     OR component LIKE CONCAT('%', #{keyword}, '%')",
            "     OR exception_class LIKE CONCAT('%', #{keyword}, '%')",
            "     OR message LIKE CONCAT('%', #{keyword}, '%')",
            "     OR request_path LIKE CONCAT('%', #{keyword}, '%'))",
            "AND (#{fromTime} IS NULL OR created_at >= #{fromTime})",
            "AND (#{toTime} IS NULL OR created_at &lt; #{toTime})",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AdminErrorLog> findPage(
            @Param("sourceType") String sourceType,
            @Param("keyword") String keyword,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM system_error_logs",
            "WHERE (#{sourceType} IS NULL OR source_type = #{sourceType})",
            "AND (#{keyword} IS NULL OR source_type LIKE CONCAT('%', #{keyword}, '%')",
            "     OR component LIKE CONCAT('%', #{keyword}, '%')",
            "     OR exception_class LIKE CONCAT('%', #{keyword}, '%')",
            "     OR message LIKE CONCAT('%', #{keyword}, '%')",
            "     OR request_path LIKE CONCAT('%', #{keyword}, '%'))",
            "AND (#{fromTime} IS NULL OR created_at >= #{fromTime})",
            "AND (#{toTime} IS NULL OR created_at &lt; #{toTime})",
            "</script>"
    })
    long count(
            @Param("sourceType") String sourceType,
            @Param("keyword") String keyword,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @Select("SELECT * FROM system_error_logs WHERE id = #{id}")
    AdminErrorLog selectDetail(@Param("id") Long id);
}
