package com.example.food.stats.image;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IngredientImageMapper extends BaseMapper<IngredientImage> {

    @Select("""
            SELECT id, canonical_name, content_type, source_provider, source_url,
                   verification_status, verification_score, failure_reason,
                   created_at, updated_at
            FROM ingredient_images
            WHERE canonical_name = #{canonicalName}
            LIMIT 1
            """)
    IngredientImage selectMetadataByCanonicalName(@Param("canonicalName") String canonicalName);

    @Select("""
            SELECT id, canonical_name, image_data, content_type, source_provider, source_url,
                   verification_status, verification_score, failure_reason,
                   created_at, updated_at
            FROM ingredient_images
            WHERE canonical_name = #{canonicalName}
            LIMIT 1
            """)
    IngredientImage selectByCanonicalName(@Param("canonicalName") String canonicalName);
}
