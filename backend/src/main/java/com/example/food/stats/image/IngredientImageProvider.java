package com.example.food.stats.image;

import java.util.List;

public interface IngredientImageProvider {

    List<IngredientImageCandidate> findCandidates(String canonicalName);

    IngredientImageContent download(IngredientImageCandidate candidate);
}
