package com.example.food.stats;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SearchLogIngredientMapper extends BaseMapper<SearchLogIngredient> {

    @Select("""
            <script>
            SELECT COUNT(DISTINCT search_log_id) AS total_searches,
                   COUNT(*) AS total_ingredient_occurrences
            FROM search_log_ingredients
            <where>
              <if test="since != null">
                created_at &gt;= #{since}
              </if>
            </where>
            </script>
            """)
    HotIngredientTotals summarize(@Param("since") LocalDateTime since);

    @Select("""
            <script>
            SELECT ingredient_name,
                   COUNT(*) AS search_count,
                   MAX(created_at) AS latest_search_at
            FROM search_log_ingredients
            <where>
              <if test="since != null">
                created_at &gt;= #{since}
              </if>
            </where>
            GROUP BY ingredient_name
            ORDER BY search_count DESC, latest_search_at DESC, ingredient_name ASC
            LIMIT #{limit}
            </script>
            """)
    List<HotIngredientAggregateRow> findHotIngredients(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit
    );
}
