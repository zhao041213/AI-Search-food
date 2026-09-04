package com.example.food.notification;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotificationScanMapper {

    @Select("""
            SELECT id
            FROM users
            WHERE role = 'USER' AND enabled = 1 AND deleted_at IS NULL
            ORDER BY id ASC
            """)
    List<Long> findActiveUserIds();
}
