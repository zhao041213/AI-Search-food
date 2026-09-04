package com.example.food.notification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_notification_preferences")
public class UserNotificationPreference {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Boolean pantryExpiringEnabled;
    private Boolean pantryExpiredEnabled;
    private Boolean weeklyMenuPreparationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getPantryExpiringEnabled() {
        return pantryExpiringEnabled;
    }

    public void setPantryExpiringEnabled(Boolean pantryExpiringEnabled) {
        this.pantryExpiringEnabled = pantryExpiringEnabled;
    }

    public Boolean getPantryExpiredEnabled() {
        return pantryExpiredEnabled;
    }

    public void setPantryExpiredEnabled(Boolean pantryExpiredEnabled) {
        this.pantryExpiredEnabled = pantryExpiredEnabled;
    }

    public Boolean getWeeklyMenuPreparationEnabled() {
        return weeklyMenuPreparationEnabled;
    }

    public void setWeeklyMenuPreparationEnabled(Boolean weeklyMenuPreparationEnabled) {
        this.weeklyMenuPreparationEnabled = weeklyMenuPreparationEnabled;
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
