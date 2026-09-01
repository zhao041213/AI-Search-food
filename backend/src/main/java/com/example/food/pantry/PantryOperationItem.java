package com.example.food.pantry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("pantry_operation_items")
public class PantryOperationItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operationId;
    private Long pantryItemId;
    private String ingredientName;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private String rawAmount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }
    public Long getPantryItemId() { return pantryItemId; }
    public void setPantryItemId(Long pantryItemId) { this.pantryItemId = pantryItemId; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getBeforeQuantity() { return beforeQuantity; }
    public void setBeforeQuantity(BigDecimal beforeQuantity) { this.beforeQuantity = beforeQuantity; }
    public BigDecimal getAfterQuantity() { return afterQuantity; }
    public void setAfterQuantity(BigDecimal afterQuantity) { this.afterQuantity = afterQuantity; }
    public String getRawAmount() { return rawAmount; }
    public void setRawAmount(String rawAmount) { this.rawAmount = rawAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
