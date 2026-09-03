package com.example.food.admin.operation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

    @Select({
            "<script>",
            "SELECT * FROM admin_operation_logs",
            "WHERE (#{result} IS NULL OR operation_result = #{result})",
            "AND (#{keyword} IS NULL OR admin_username LIKE CONCAT('%', #{keyword}, '%')",
            "     OR operation_type LIKE CONCAT('%', #{keyword}, '%')",
            "     OR request_path LIKE CONCAT('%', #{keyword}, '%'))",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AdminOperationLog> findPage(
            @Param("keyword") String keyword,
            @Param("result") String result,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM admin_operation_logs",
            "WHERE (#{result} IS NULL OR operation_result = #{result})",
            "AND (#{keyword} IS NULL OR admin_username LIKE CONCAT('%', #{keyword}, '%')",
            "     OR operation_type LIKE CONCAT('%', #{keyword}, '%')",
            "     OR request_path LIKE CONCAT('%', #{keyword}, '%'))",
            "</script>"
    })
    long count(
            @Param("keyword") String keyword,
            @Param("result") String result
    );
}
