package com.example.food.recipe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("recipe_ingredients")
public class RecipeIngredient {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipeId;
    private String ingredientName;
    private String amount;
    private String category;
    private Boolean alreadyOwned;
    private String substituteNames;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getAlreadyOwned() {
        return alreadyOwned;
    }

    public void setAlreadyOwned(Boolean alreadyOwned) {
        this.alreadyOwned = alreadyOwned;
    }

    public String getSubstituteNames() {
        return substituteNames;
    }

    public void setSubstituteNames(String substituteNames) {
        this.substituteNames = substituteNames;
    }
}
