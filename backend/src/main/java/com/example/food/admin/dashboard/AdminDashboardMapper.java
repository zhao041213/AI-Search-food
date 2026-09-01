package com.example.food.admin.dashboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminDashboardMapper {

    @Select("""
            SELECT COUNT(*)
            FROM users
            WHERE created_at >= #{since} AND created_at < #{until}
            """)
    long countNewUsers(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Select("""
            SELECT COUNT(*)
            FROM search_logs
            WHERE created_at >= #{since} AND created_at < #{until}
            """)
    long countGenerations(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Select("""
            SELECT COUNT(*)
            FROM recipe_records
            WHERE created_at >= #{since} AND created_at < #{until}
            """)
    long countSavedRecipes(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Select("""
            SELECT COUNT(*)
            FROM finished_dish_reviews
            WHERE created_at >= #{since} AND created_at < #{until}
            """)
    long countReviews(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Select("""
            SELECT CAST(created_at AS DATE) AS trend_date, COUNT(*) AS count
            FROM search_logs
            WHERE created_at >= #{since} AND created_at < #{until}
            GROUP BY CAST(created_at AS DATE)
            ORDER BY trend_date ASC
            """)
    List<AdminDashboardDailyAggregateRow> findDailyGenerations(
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until
    );

    @Select("""
            SELECT CAST(created_at AS DATE) AS trend_date, COUNT(*) AS count
            FROM recipe_records
            WHERE created_at >= #{since} AND created_at < #{until}
            GROUP BY CAST(created_at AS DATE)
            ORDER BY trend_date ASC
            """)
    List<AdminDashboardDailyAggregateRow> findDailySavedRecipes(
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until
    );

    @Select("""
            SELECT COALESCE(NULLIF(TRIM(input_type), ''), 'other') AS input_type,
                   COUNT(*) AS count
            FROM search_logs
            WHERE created_at >= #{since} AND created_at < #{until}
            GROUP BY COALESCE(NULLIF(TRIM(input_type), ''), 'other')
            ORDER BY count DESC, input_type ASC
            """)
    List<AdminDashboardInputSourceRow> findInputSources(
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until
    );

    @Select("""
            SELECT ingredient_name AS name, COUNT(*) AS count
            FROM search_log_ingredients
            WHERE created_at >= #{since} AND created_at < #{until}
            GROUP BY ingredient_name
            ORDER BY count DESC, name ASC
            LIMIT #{limit}
            """)
    List<AdminDashboardHotIngredientRow> findHotIngredients(
            @Param("since") LocalDateTime since,
            @Param("until") LocalDateTime until,
            @Param("limit") int limit
    );
}
