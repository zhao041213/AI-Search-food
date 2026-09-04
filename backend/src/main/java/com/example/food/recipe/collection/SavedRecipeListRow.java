package com.example.food.recipe.collection;

import java.time.LocalDateTime;

public class SavedRecipeListRow {

    private Long id;
    private String title;
    private String summary;
    private String searchIngredients;
    private String mealType;
    private String goal;
    private String coverIngredient;
    private LocalDateTime savedAt;
    private Long collectionId;
    private String collectionName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSearchIngredients() {
        return searchIngredients;
    }

    public void setSearchIngredients(String searchIngredients) {
        this.searchIngredients = searchIngredients;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getCoverIngredient() {
        return coverIngredient;
    }

    public void setCoverIngredient(String coverIngredient) {
        this.coverIngredient = coverIngredient;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
