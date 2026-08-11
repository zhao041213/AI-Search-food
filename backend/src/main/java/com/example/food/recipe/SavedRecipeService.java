package com.example.food.recipe;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.RecipeHistorySummaryResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SavedRecipeService {

    private final SearchLogMapper searchLogMapper;
    private final RecipeRecordMapper recipeRecordMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final ObjectMapper objectMapper;

    public SavedRecipeService(
            SearchLogMapper searchLogMapper,
            RecipeRecordMapper recipeRecordMapper,
            RecipeIngredientMapper recipeIngredientMapper,
            RecipeStepMapper recipeStepMapper,
            ObjectMapper objectMapper
    ) {
        this.searchLogMapper = searchLogMapper;
        this.recipeRecordMapper = recipeRecordMapper;
        this.recipeIngredientMapper = recipeIngredientMapper;
        this.recipeStepMapper = recipeStepMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecipeHistoryDetailResponse save(
            SaveRecipeRequest request,
            AuthPrincipal principal,
            String anonymousId
    ) {
        requireUser(principal);
        SearchLog searchLog = searchLogMapper.selectById(request.searchLogId());
        if (searchLog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "搜索记录不存在");
        }
        verifySearchLogOwnership(searchLog, principal, anonymousId);

        RecipeGenerateResponse recipe = recipeFromRequest(request);
        RecipeRecord record = new RecipeRecord();
        record.setUserId(principal.id());
        record.setSearchLogId(request.searchLogId());
        record.setTitle(request.title().trim());
        record.setSummary(request.summary());
        record.setEffects(toJson(request.effects()));
        record.setTips(toJson(request.tips()));
        record.setVideoKeywords(toJson(request.videoKeywords()));
        record.setAiModel(modelLabel(request.provider(), request.model()));
        record.setRawResponse(toJson(recipe));
        record.setCreatedAt(LocalDateTime.now());
        recipeRecordMapper.insert(record);

        insertIngredients(record.getId(), request.ingredients());
        insertSteps(record.getId(), request.steps());
        return detailResponse(record, searchLog, recipe);
    }

    public List<RecipeHistorySummaryResponse> list(Long userId, int limit, int offset) {
        validatePage(limit, offset);
        List<RecipeRecord> records = recipeRecordMapper.selectList(new QueryWrapper<RecipeRecord>()
                .eq("user_id", userId)
                .orderByDesc("created_at")
                .last("LIMIT " + limit + " OFFSET " + offset));
        return records.stream().map(this::summaryResponse).toList();
    }

    public RecipeHistoryDetailResponse detail(Long userId, Long recipeId) {
        RecipeRecord record = recipeRecordMapper.selectById(recipeId);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "菜谱不存在");
        }
        if (!userId.equals(record.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该菜谱");
        }

        SearchLog searchLog = record.getSearchLogId() == null
                ? null
                : searchLogMapper.selectById(record.getSearchLogId());
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(new QueryWrapper<RecipeIngredient>()
                .eq("recipe_id", recipeId)
                .orderByAsc("id"));
        List<RecipeStep> steps = recipeStepMapper.selectList(new QueryWrapper<RecipeStep>()
                .eq("recipe_id", recipeId)
                .orderByAsc("step_no"));
        RecipeGenerateResponse recipe = recipeFromRecord(record, ingredients, steps);
        return detailResponse(record, searchLog, recipe);
    }

    private void insertIngredients(Long recipeId, List<RecipeGenerateResponse.Ingredient> ingredients) {
        for (RecipeGenerateResponse.Ingredient ingredient : ingredients) {
            RecipeIngredient target = new RecipeIngredient();
            target.setRecipeId(recipeId);
            target.setIngredientName(requireText(ingredient.name(), "食材名称不能为空"));
            target.setAmount(ingredient.amount());
            target.setAlreadyOwned(false);
            recipeIngredientMapper.insert(target);
        }
    }

    private void insertSteps(Long recipeId, List<RecipeGenerateResponse.Step> steps) {
        for (RecipeGenerateResponse.Step step : steps) {
            RecipeStep target = new RecipeStep();
            target.setRecipeId(recipeId);
            target.setStepNo(step.order());
            target.setTitle(step.title());
            target.setInstruction(requireText(step.description(), "烹饪步骤不能为空"));
            target.setEstimatedMinutes(step.durationMinutes());
            recipeStepMapper.insert(target);
        }
    }

    private RecipeHistorySummaryResponse summaryResponse(RecipeRecord record) {
        SearchLog searchLog = record.getSearchLogId() == null
                ? null
                : searchLogMapper.selectById(record.getSearchLogId());
        return new RecipeHistorySummaryResponse(
                record.getId(),
                record.getTitle(),
                searchLog == null ? null : searchLog.getQueryText(),
                searchLog == null ? null : searchLog.getMealType(),
                searchLog == null ? null : searchLog.getGoal(),
                record.getCreatedAt()
        );
    }

    private RecipeHistoryDetailResponse detailResponse(
            RecipeRecord record,
            SearchLog searchLog,
            RecipeGenerateResponse recipe
    ) {
        return new RecipeHistoryDetailResponse(
                record.getId(),
                record.getSearchLogId(),
                record.getCreatedAt(),
                searchLog == null ? null : searchLog.getQueryText(),
                searchLog == null ? null : searchLog.getMealType(),
                searchLog == null ? null : searchLog.getGoal(),
                recipe
        );
    }

    private RecipeGenerateResponse recipeFromRequest(SaveRecipeRequest request) {
        return new RecipeGenerateResponse(
                request.title(),
                request.summary(),
                request.effects(),
                request.ingredients(),
                request.steps(),
                request.tips(),
                request.videoKeywords(),
                request.provider(),
                request.model(),
                request.searchLogId()
        );
    }

    private RecipeGenerateResponse recipeFromRecord(
            RecipeRecord record,
            List<RecipeIngredient> ingredients,
            List<RecipeStep> steps
    ) {
        List<RecipeGenerateResponse.Ingredient> recipeIngredients = ingredients.stream()
                .map(item -> new RecipeGenerateResponse.Ingredient(item.getIngredientName(), item.getAmount()))
                .toList();
        List<RecipeGenerateResponse.Step> recipeSteps = steps.stream()
                .map(item -> new RecipeGenerateResponse.Step(
                        item.getStepNo(),
                        item.getTitle(),
                        item.getInstruction(),
                        item.getEstimatedMinutes()
                ))
                .toList();
        return new RecipeGenerateResponse(
                record.getTitle(),
                record.getSummary(),
                readList(record.getEffects()),
                recipeIngredients,
                recipeSteps,
                readList(record.getTips()),
                readList(record.getVideoKeywords()),
                provider(record.getAiModel()),
                model(record.getAiModel()),
                record.getSearchLogId()
        );
    }

    private void verifySearchLogOwnership(SearchLog searchLog, AuthPrincipal principal, String anonymousId) {
        if (searchLog.getUserId() != null && !principal.id().equals(searchLog.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该搜索记录");
        }
        if (searchLog.getUserId() == null
                && (!StringUtils.hasText(anonymousId)
                || !anonymousId.trim().equals(searchLog.getAnonymousId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该搜索记录");
        }
    }

    private void requireUser(AuthPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (principal.role() != AppRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅普通用户可以保存菜谱");
        }
    }

    private void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 50 || offset < 0) {
            throw new IllegalArgumentException("分页参数不合法");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("菜谱内容序列化失败", exception);
        }
    }

    private List<String> readList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("菜谱内容解析失败", exception);
        }
    }

    private String modelLabel(String provider, String model) {
        return provider.trim() + ":" + model.trim();
    }

    private String provider(String modelLabel) {
        if (!StringUtils.hasText(modelLabel) || !modelLabel.contains(":")) {
            return modelLabel;
        }
        return modelLabel.substring(0, modelLabel.indexOf(':'));
    }

    private String model(String modelLabel) {
        if (!StringUtils.hasText(modelLabel) || !modelLabel.contains(":")) {
            return modelLabel;
        }
        return modelLabel.substring(modelLabel.indexOf(':') + 1);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
