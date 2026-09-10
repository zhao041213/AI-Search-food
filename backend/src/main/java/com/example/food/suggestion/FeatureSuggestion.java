package com.example.food.suggestion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("feature_suggestions")
public class FeatureSuggestion {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String type;
    private String title;
    private String detail;
    private String expectedEffect;
    private String status;
    private String screenshotOriginalName;
    private String screenshotStoredName;
    private String screenshotContentType;
    private Long screenshotSize;
    private String screenshotStoragePath;
    private Long duplicateOfId;
    private String internalNote;
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastProcessedAt;
    private LocalDateTime deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(String expectedEffect) { this.expectedEffect = expectedEffect; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getScreenshotOriginalName() { return screenshotOriginalName; }
    public void setScreenshotOriginalName(String screenshotOriginalName) { this.screenshotOriginalName = screenshotOriginalName; }
    public String getScreenshotStoredName() { return screenshotStoredName; }
    public void setScreenshotStoredName(String screenshotStoredName) { this.screenshotStoredName = screenshotStoredName; }
    public String getScreenshotContentType() { return screenshotContentType; }
    public void setScreenshotContentType(String screenshotContentType) { this.screenshotContentType = screenshotContentType; }
    public Long getScreenshotSize() { return screenshotSize; }
    public void setScreenshotSize(Long screenshotSize) { this.screenshotSize = screenshotSize; }
    public String getScreenshotStoragePath() { return screenshotStoragePath; }
    public void setScreenshotStoragePath(String screenshotStoragePath) { this.screenshotStoragePath = screenshotStoragePath; }
    public Long getDuplicateOfId() { return duplicateOfId; }
    public void setDuplicateOfId(Long duplicateOfId) { this.duplicateOfId = duplicateOfId; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastProcessedAt() { return lastProcessedAt; }
    public void setLastProcessedAt(LocalDateTime lastProcessedAt) { this.lastProcessedAt = lastProcessedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
