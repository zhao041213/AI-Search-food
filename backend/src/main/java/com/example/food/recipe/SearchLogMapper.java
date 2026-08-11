package com.example.food.recipe;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {

    @Select("""
            SELECT sl.*
            FROM search_logs sl
            WHERE sl.recognized_ingredients IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM search_log_ingredients sli
                  WHERE sli.search_log_id = sl.id
              )
            ORDER BY sl.id
            """)
    List<SearchLog> findWithoutIngredientDetails();
}
