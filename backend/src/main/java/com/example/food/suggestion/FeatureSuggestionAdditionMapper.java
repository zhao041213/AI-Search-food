package com.example.food.suggestion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FeatureSuggestionAdditionMapper extends BaseMapper<FeatureSuggestionAddition> {
    @Select("SELECT * FROM feature_suggestion_additions WHERE suggestion_id = #{suggestionId} ORDER BY created_at ASC, id ASC")
    List<FeatureSuggestionAddition> findBySuggestionId(@Param("suggestionId") Long suggestionId);
}
