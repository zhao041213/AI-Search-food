package com.example.food.recipe.collection;

public class RecipeTagSummaryRow {

    private Long id;
    private String name;
    private long recipeCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getRecipeCount() {
        return recipeCount;
    }

    public void setRecipeCount(long recipeCount) {
        this.recipeCount = recipeCount;
    }
}
