package com.example.food.recipe.collection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RecipeCollectionMapper extends BaseMapper<RecipeCollection> {

    @Select("""
            SELECT id, user_id, name, is_default AS default_collection, created_at, updated_at
            FROM recipe_collections
            WHERE user_id = #{userId}
            ORDER BY is_default DESC, created_at ASC, id ASC
            """)
    List<RecipeCollection> findByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, name, is_default AS default_collection, created_at, updated_at
            FROM recipe_collections
            WHERE user_id = #{userId}
              AND is_default = 1
            ORDER BY id ASC
            LIMIT 1
            """)
    RecipeCollection findDefaultByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, name, is_default AS default_collection, created_at, updated_at
            FROM recipe_collections
            WHERE id = #{collectionId}
              AND user_id = #{userId}
            """)
    RecipeCollection findOwned(
            @Param("userId") Long userId,
            @Param("collectionId") Long collectionId
    );

    @Select("""
            SELECT id, user_id, name, is_default AS default_collection, created_at, updated_at
            FROM recipe_collections
            WHERE user_id = #{userId}
              AND name = #{name}
            LIMIT 1
            """)
    RecipeCollection findByUserIdAndName(
            @Param("userId") Long userId,
            @Param("name") String name
    );

    @Select("SELECT COUNT(*) FROM recipe_collections WHERE user_id = #{userId} AND is_default = 0")
    int countCustomByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM recipe_records rr
            LEFT JOIN recipe_collection_items rci
              ON rci.recipe_id = rr.id
             AND rci.user_id = #{userId}
            WHERE rr.user_id = #{userId}
              AND (rci.id IS NULL OR rci.collection_id = #{collectionId})
            """)
    int countDefaultRecipes(
            @Param("userId") Long userId,
            @Param("collectionId") Long collectionId
    );

    @Select("""
            SELECT COUNT(*)
            FROM recipe_records rr
            INNER JOIN recipe_collection_items rci
              ON rci.recipe_id = rr.id
             AND rci.user_id = #{userId}
            WHERE rr.user_id = #{userId}
              AND rci.collection_id = #{collectionId}
            """)
    int countCustomRecipes(
            @Param("userId") Long userId,
            @Param("collectionId") Long collectionId
    );

    @Update("""
            UPDATE recipe_collections
            SET name = #{name}, updated_at = #{updatedAt}
            WHERE id = #{collectionId}
              AND user_id = #{userId}
              AND is_default = 0
            """)
    int renameOwnedCustom(
            @Param("userId") Long userId,
            @Param("collectionId") Long collectionId,
            @Param("name") String name,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );
}
