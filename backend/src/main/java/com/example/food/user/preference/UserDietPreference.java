package com.example.food.user.preference;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_diet_preferences")
public class UserDietPreference {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private String taste;
    private String defaultGoal;
    private String avoidIngredients;
    private String allergenIngredients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTaste() {
        return taste;
    }

    public void setTaste(String taste) {
        this.taste = taste;
    }

    public String getDefaultGoal() {
        return defaultGoal;
    }

    public void setDefaultGoal(String defaultGoal) {
        this.defaultGoal = defaultGoal;
    }

    public String getAvoidIngredients() {
        return avoidIngredients;
    }

    public void setAvoidIngredients(String avoidIngredients) {
        this.avoidIngredients = avoidIngredients;
    }

    public String getAllergenIngredients() {
        return allergenIngredients;
    }

    public void setAllergenIngredients(String allergenIngredients) {
        this.allergenIngredients = allergenIngredients;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
