package com.example.food.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UploadedFileMapper extends BaseMapper<UploadedFile> {

    @Select("SELECT * FROM uploaded_files WHERE user_id = #{userId} AND purpose = #{purpose} ORDER BY id DESC LIMIT 1")
    UploadedFile selectLatestByUserAndPurpose(@Param("userId") Long userId, @Param("purpose") String purpose);

    @Select("SELECT * FROM uploaded_files WHERE user_id = #{userId} AND purpose = #{purpose}")
    java.util.List<UploadedFile> selectByUserAndPurpose(@Param("userId") Long userId, @Param("purpose") String purpose);
}
