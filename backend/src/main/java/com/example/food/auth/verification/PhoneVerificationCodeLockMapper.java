package com.example.food.auth.verification;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PhoneVerificationCodeLockMapper {

    @Insert("""
            INSERT INTO phone_verification_code_locks (phone, purpose)
            VALUES (#{phone}, #{purpose})
            ON DUPLICATE KEY UPDATE purpose = VALUES(purpose)
            """)
    int ensureLockRow(@Param("phone") String phone, @Param("purpose") String purpose);

    @Select("""
            SELECT phone
            FROM phone_verification_code_locks
            WHERE phone = #{phone} AND purpose = #{purpose}
            FOR UPDATE
            """)
    String lock(@Param("phone") String phone, @Param("purpose") String purpose);
}
