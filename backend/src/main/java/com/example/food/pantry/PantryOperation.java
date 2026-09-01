package com.example.food.pantry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("pantry_operations")
public class PantryOperation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String operationType;
    private String sourceType;
    private Long sourceId;
    private String idempotencyKey;
    private String status;
    private Integer actualServings;
    private Long reversedOperationId;
    private LocalDateTime createdAt;
    private LocalDateTime reversedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getActualServings() { return actualServings; }
    public void setActualServings(Integer actualServings) { this.actualServings = actualServings; }
    public Long getReversedOperationId() { return reversedOperationId; }
    public void setReversedOperationId(Long reversedOperationId) { this.reversedOperationId = reversedOperationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReversedAt() { return reversedAt; }
    public void setReversedAt(LocalDateTime reversedAt) { this.reversedAt = reversedAt; }
}
