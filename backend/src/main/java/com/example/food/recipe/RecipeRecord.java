package com.example.food.recipe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("recipe_records")
public class RecipeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long searchLogId;
    private Long parentRecipeId;
    private String title;
    private String summary;
    private String effects;
    private String tips;
    private String videoKeywords;
    private String aiModel;
    private String rawResponse;
    private LocalDateTime createdAt;

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

    public Long getSearchLogId() {
        return searchLogId;
    }

    public void setSearchLogId(Long searchLogId) {
        this.searchLogId = searchLogId;
    }

    public Long getParentRecipeId() {
        return parentRecipeId;
    }

    public void setParentRecipeId(Long parentRecipeId) {
        this.parentRecipeId = parentRecipeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEffects() {
        return effects;
    }

    public void setEffects(String effects) {
        this.effects = effects;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public String getVideoKeywords() {
        return videoKeywords;
    }

    public void setVideoKeywords(String videoKeywords) {
        this.videoKeywords = videoKeywords;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
