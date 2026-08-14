# 用户偏好、最近搜索与菜谱管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为普通用户增加可持久化饮食偏好、搜索框最近 5 条历史回填，以及个人菜谱搜索、筛选和删除能力。

**Architecture:** 使用独立的 `user_diet_preferences` 表保存用户偏好，最近搜索直接读取现有 `search_logs` 且不裁剪统计数据。菜谱列表通过 `recipe_records` 与 `search_logs` 的后端关联查询完成筛选，前端使用独立偏好弹层和最近搜索下拉组件接入现有工作台。

**Tech Stack:** Java 17、Spring Boot 3.3、Spring Security JWT、MyBatis-Plus、Flyway、H2/MySQL、Vue 3、Pinia、Element Plus、Axios、Vite、Node Test Runner。

---

## Scope Guard

- 只实现饮食偏好、最近搜索 5 条和我的菜谱管理。
- 不删除超过 5 条的 `search_logs`，避免热门食材统计失真。
- 不实现游客本地历史、拍照识别、成品图评价、烹饪步骤模式或 APK。
- 不重构认证、热门排行和无关前端页面。
- 计划中的提交步骤只有在用户明确批准时执行；否则保留为未提交改动。

## File Map

Backend create:

- `backend/src/main/resources/db/migration/V8__add_user_diet_preferences.sql`：偏好表与搜索历史索引。
- `backend/src/main/java/com/example/food/user/preference/UserDietPreference.java`：偏好持久化实体。
- `backend/src/main/java/com/example/food/user/preference/UserDietPreferenceMapper.java`：偏好 Mapper。
- `backend/src/main/java/com/example/food/user/preference/UserDietPreferenceService.java`：偏好读取、规范化和覆盖保存。
- `backend/src/main/java/com/example/food/user/preference/UserDietPreferenceController.java`：当前用户偏好接口。
- `backend/src/main/java/com/example/food/user/preference/dto/DietPreferenceRequest.java`：偏好保存请求。
- `backend/src/main/java/com/example/food/user/preference/dto/DietPreferenceResponse.java`：偏好响应。
- `backend/src/main/java/com/example/food/recipe/SearchHistoryService.java`：最近搜索查询和去重。
- `backend/src/main/java/com/example/food/recipe/SearchHistoryController.java`：最近搜索接口。
- `backend/src/main/java/com/example/food/recipe/dto/RecentSearchResponse.java`：最近搜索响应。
- `backend/src/test/java/com/example/food/user/preference/UserDietPreferenceServiceTest.java`。
- `backend/src/test/java/com/example/food/user/preference/UserDietPreferenceControllerTest.java`。
- `backend/src/test/java/com/example/food/recipe/SearchHistoryServiceTest.java`。
- `backend/src/test/java/com/example/food/recipe/SearchHistoryControllerTest.java`。

Backend modify:

- `backend/src/main/java/com/example/food/security/SecurityConfig.java`：保护偏好和搜索历史接口。
- `backend/src/main/java/com/example/food/ai/recipe/dto/RecipeGenerateRequest.java`：增加可选饮食偏好契约。
- `backend/src/main/java/com/example/food/ai/recipe/RecipeRecommendationService.java`：将偏好写入生成和再生成提示词。
- `backend/src/main/java/com/example/food/recipe/RecipeRecordMapper.java`：增加当前用户菜谱关联筛选查询。
- `backend/src/main/java/com/example/food/recipe/SavedRecipeService.java`：增加筛选参数和归属校验删除。
- `backend/src/main/java/com/example/food/recipe/SavedRecipeController.java`：扩展列表参数并增加删除接口。
- `backend/src/test/java/com/example/food/recipe/PersistenceSchemaTest.java`。
- `backend/src/test/java/com/example/food/security/SecurityConfigTest.java`。
- `backend/src/test/java/com/example/food/ai/recipe/RecipeRecommendationServiceTest.java`。
- `backend/src/test/java/com/example/food/ai/recipe/RecipeControllerTest.java`。
- `backend/src/test/java/com/example/food/recipe/SavedRecipeServiceTest.java`。
- `backend/src/test/java/com/example/food/recipe/SavedRecipeControllerTest.java`。
- `backend/src/test/java/com/example/food/recipe/RecipePersistenceIntegrationTest.java`。

Frontend create:

- `frontend/src/api/userPreferences.js`：偏好读取和保存 API。
- `frontend/src/api/searchHistory.js`：最近搜索 API。
- `frontend/src/components/DietPreferenceDialog.vue`：偏好编辑弹层。
- `frontend/src/components/RecentSearchPopover.vue`：最近搜索下拉。
- `frontend/src/utils/personalization.js`：偏好与历史回填纯函数。
- `frontend/src/utils/personalization.test.js`：纯函数测试。

Frontend modify:

- `frontend/src/api/recipes.js`：删除菜谱和筛选参数。
- `frontend/src/views/HomeView.vue`：加载偏好、展示历史、提交生成参数。
- `frontend/src/views/SavedRecipesView.vue`：搜索、筛选和删除交互。

---

### Task 1: 数据库迁移与偏好持久化模型

**Files:**

- Create: `backend/src/main/resources/db/migration/V8__add_user_diet_preferences.sql`
- Create: `backend/src/main/java/com/example/food/user/preference/UserDietPreference.java`
- Create: `backend/src/main/java/com/example/food/user/preference/UserDietPreferenceMapper.java`
- Modify: `backend/src/test/java/com/example/food/recipe/PersistenceSchemaTest.java`

- [ ] **Step 1: 先写失败的迁移测试**

在 `PersistenceSchemaTest` 增加断言：

```java
@Test
void createsDietPreferenceTableAndSearchHistoryIndex() {
    Integer tableCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_NAME = 'USER_DIET_PREFERENCES'
            """, Integer.class);
    Integer indexCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES
            WHERE INDEX_NAME = 'IDX_SEARCH_LOGS_USER_CREATED'
            """, Integer.class);

    assertThat(tableCount).isEqualTo(1);
    assertThat(indexCount).isEqualTo(1);
}
```

- [ ] **Step 2: 运行迁移测试并确认 RED**

Run:

```powershell
rtk mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" "-Dtest=PersistenceSchemaTest" test
```

Expected: FAIL，因为表和索引尚不存在。

- [ ] **Step 3: 创建 V8 迁移**

`V8__add_user_diet_preferences.sql`：

```sql
CREATE TABLE user_diet_preferences (
    user_id BIGINT PRIMARY KEY,
    taste VARCHAR(32),
    default_goal VARCHAR(64),
    avoid_ingredients TEXT NOT NULL,
    allergen_ingredients TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_diet_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_search_logs_user_created
    ON search_logs(user_id, created_at);
```

- [ ] **Step 4: 创建实体与 Mapper**

实体核心结构：

```java
@TableName("user_diet_preferences")
public class UserDietPreference {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private String taste;
    private String defaultGoal;
    private String avoidIngredients;
    private String allergenIngredients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // standard getters and setters
}
```

Mapper：

```java
@Mapper
public interface UserDietPreferenceMapper extends BaseMapper<UserDietPreference> {
}
```

- [ ] **Step 5: 运行迁移测试并确认 GREEN**

运行 Step 2 命令，Expected: PASS。

- [ ] **Step 6: 用户批准后提交本任务**

```powershell
rtk git add backend/src/main/resources/db/migration/V8__add_user_diet_preferences.sql backend/src/main/java/com/example/food/user/preference backend/src/test/java/com/example/food/recipe/PersistenceSchemaTest.java
rtk git commit -m "feat: 增加用户饮食偏好存储"
```

---

### Task 2: 用户饮食偏好接口

**Files:**

- Create: `backend/src/main/java/com/example/food/user/preference/dto/DietPreferenceRequest.java`
- Create: `backend/src/main/java/com/example/food/user/preference/dto/DietPreferenceResponse.java`
- Create: `backend/src/main/java/com/example/food/user/preference/UserDietPreferenceService.java`
- Create: `backend/src/main/java/com/example/food/user/preference/UserDietPreferenceController.java`
- Create: `backend/src/test/java/com/example/food/user/preference/UserDietPreferenceServiceTest.java`
- Create: `backend/src/test/java/com/example/food/user/preference/UserDietPreferenceControllerTest.java`
- Modify: `backend/src/main/java/com/example/food/security/SecurityConfig.java`
- Modify: `backend/src/test/java/com/example/food/security/SecurityConfigTest.java`

- [ ] **Step 1: 写偏好服务失败测试**

覆盖默认值、规范化、覆盖保存和用户隔离：

```java
@Test
void returnsEmptyPreferenceWhenUserHasNoRecord() {
    when(mapper.selectById(7L)).thenReturn(null);

    DietPreferenceResponse result = service.get(7L);

    assertThat(result.taste()).isEqualTo("any");
    assertThat(result.defaultGoal()).isEqualTo("balanced");
    assertThat(result.avoidIngredients()).isEmpty();
    assertThat(result.allergenIngredients()).isEmpty();
}

@Test
void normalizesAndUpdatesPreference() {
    UserDietPreference existing = preference(7L);
    when(mapper.selectById(7L)).thenReturn(existing);

    service.save(7L, new DietPreferenceRequest(
            "light", "balanced",
            List.of(" 香菜 ", "香菜", ""),
            List.of("花生", " 花生 ")
    ));

    verify(mapper).updateById(argThat(saved ->
            saved.getUserId().equals(7L)
                    && saved.getAvoidIngredients().equals("[\"香菜\"]")
                    && saved.getAllergenIngredients().equals("[\"花生\"]")
    ));
}
```

- [ ] **Step 2: 运行偏好测试并确认 RED**

```powershell
rtk mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" "-Dtest=UserDietPreferenceServiceTest,UserDietPreferenceControllerTest,SecurityConfigTest" test
```

Expected: FAIL，因为服务、控制器和安全规则尚不存在。

- [ ] **Step 3: 实现请求与响应 DTO**

```java
public record DietPreferenceRequest(
        @Pattern(regexp = "any|light|home|spicy|sweet_sour") String taste,
        @Pattern(regexp = "balanced|fat_loss|muscle_gain|low_sugar") String defaultGoal,
        @Size(max = 20) List<@Size(max = 32) String> avoidIngredients,
        @Size(max = 20) List<@Size(max = 32) String> allergenIngredients
) {
}

public record DietPreferenceResponse(
        String taste,
        String defaultGoal,
        List<String> avoidIngredients,
        List<String> allergenIngredients
) {
    public static DietPreferenceResponse empty() {
        return new DietPreferenceResponse("any", "balanced", List.of(), List.of());
    }
}
```

- [ ] **Step 4: 实现服务 upsert**

`save()` 必须：

1. `trim()` 文本。
2. 去除空项并按首次出现顺序去重。
3. 每个列表最多 20 项，每项最多 32 字符。
4. 用 `ObjectMapper` 存储 JSON 数组。
5. 按 `userId` 查询；不存在 `insert`，存在 `updateById`。

方法签名：

```java
public DietPreferenceResponse get(Long userId)

@Transactional
public DietPreferenceResponse save(Long userId, DietPreferenceRequest request)
```

- [ ] **Step 5: 实现控制器和安全规则**

```java
@RestController
@RequestMapping("/api/users/me/diet-preferences")
public class UserDietPreferenceController {
    @GetMapping
    public ApiResponse<DietPreferenceResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.get(principal.id()));
    }

    @PutMapping
    public ApiResponse<DietPreferenceResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody DietPreferenceRequest request) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }
}
```

在 `SecurityConfig` 的管理员规则之后增加：

```java
.requestMatchers("/api/users/me/**").hasRole("USER")
.requestMatchers("/api/search-history/**").hasRole("USER")
```

- [ ] **Step 6: 运行测试并确认 GREEN**

运行 Step 2 命令，Expected: PASS。

- [ ] **Step 7: 用户批准后提交本任务**

```powershell
rtk git add backend/src/main/java/com/example/food/user/preference backend/src/main/java/com/example/food/security/SecurityConfig.java backend/src/test/java/com/example/food/user/preference backend/src/test/java/com/example/food/security/SecurityConfigTest.java
rtk git commit -m "feat: 增加用户饮食偏好接口"
```

---

### Task 3: 将饮食偏好接入 AI 生成和再生成

**Files:**

- Modify: `backend/src/main/java/com/example/food/ai/recipe/dto/RecipeGenerateRequest.java`
- Modify: `backend/src/main/java/com/example/food/ai/recipe/RecipeRecommendationService.java`
- Modify: `backend/src/test/java/com/example/food/ai/recipe/RecipeRecommendationServiceTest.java`
- Modify: `backend/src/test/java/com/example/food/ai/recipe/RecipeControllerTest.java`

- [ ] **Step 1: 写提示词和契约失败测试**

```java
@Test
void includesDietPreferenceInPrompt() {
    RecipeGenerateRequest request = new RecipeGenerateRequest(
            "番茄, 鸡蛋", "dinner", "balanced", "text", null, null,
            new RecipeGenerateRequest.DietPreference(
                    "light", "balanced", List.of("香菜"), List.of("花生")
            )
    );

    service.generate(request);

    verify(qwenRecipeClient).generateRecipe(argThat(prompt ->
            prompt.contains("清淡")
                    && prompt.contains("不得使用过敏食材：花生")
                    && prompt.contains("排除忌口食材：香菜")
    ));
}
```

控制器测试提交完整 `dietPreference` JSON 并验证 200。

- [ ] **Step 2: 运行聚焦测试并确认 RED**

```powershell
rtk mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" "-Dtest=RecipeRecommendationServiceTest,RecipeControllerTest" test
```

Expected: FAIL，因为请求契约没有 `dietPreference`。

- [ ] **Step 3: 扩展请求契约并保持兼容构造器**

```java
public record RecipeGenerateRequest(
        @NotBlank @Size(max = 240) String ingredients,
        String mealType,
        String goal,
        String searchMode,
        String regenerationPreference,
        String previousTitle,
        @Valid DietPreference dietPreference
) {
    public record DietPreference(
            @Size(max = 32) String taste,
            @Size(max = 64) String defaultGoal,
            @Size(max = 20) List<@Size(max = 32) String> avoidIngredients,
            @Size(max = 20) List<@Size(max = 32) String> allergenIngredients
    ) {
    }
}
```

保留现有四参数和六参数构造器，并把新增参数设为 `null`，避免现有测试和调用方破坏。

- [ ] **Step 4: 更新提示词**

在 `buildPrompt()` 增加独立偏好段：

```text
用户饮食偏好：口味=%s；默认目标=%s。
排除忌口食材：%s。
不得使用过敏食材：%s。
过敏约束优先级最高；不得通过“少量使用”规避。若无法满足，返回不包含禁用食材的替代菜谱。
```

空偏好不输出冗余段落。口味枚举在服务中映射为中文，避免模型接收内部代码。

- [ ] **Step 5: 运行聚焦测试并确认 GREEN**

运行 Step 2 命令，Expected: PASS。

- [ ] **Step 6: 用户批准后提交本任务**

```powershell
rtk git add backend/src/main/java/com/example/food/ai/recipe backend/src/test/java/com/example/food/ai/recipe
rtk git commit -m "feat: 将饮食偏好接入菜谱生成"
```

---

### Task 4: 当前用户最近 5 条搜索接口

**Files:**

- Create: `backend/src/main/java/com/example/food/recipe/dto/RecentSearchResponse.java`
- Create: `backend/src/main/java/com/example/food/recipe/SearchHistoryService.java`
- Create: `backend/src/main/java/com/example/food/recipe/SearchHistoryController.java`
- Create: `backend/src/test/java/com/example/food/recipe/SearchHistoryServiceTest.java`
- Create: `backend/src/test/java/com/example/food/recipe/SearchHistoryControllerTest.java`

- [ ] **Step 1: 写最近搜索失败测试**

```java
@Test
void returnsLatestFiveDistinctSearchesForCurrentUser() {
    when(mapper.selectList(any())).thenReturn(List.of(
            log(9L, 7L, "番茄、鸡蛋", "dinner", "balanced", "text", time(9)),
            log(8L, 7L, "番茄、鸡蛋", "dinner", "balanced", "text", time(8)),
            log(7L, 7L, "牛肉", "lunch", "muscle_gain", "text", time(7)),
            log(6L, 7L, "土豆", "dinner", "balanced", "image", time(6)),
            log(5L, 7L, "鸡胸肉", "lunch", "fat_loss", "text", time(5)),
            log(4L, 7L, "西兰花", "dinner", "balanced", "text", time(4)),
            log(3L, 7L, "豆腐", "breakfast", "balanced", "text", time(3))
    ));

    List<RecentSearchResponse> result = service.recent(7L);

    assertThat(result).hasSize(5);
    assertThat(result).extracting(RecentSearchResponse::id)
            .containsExactly(9L, 7L, 6L, 5L, 4L);
}
```

- [ ] **Step 2: 运行测试并确认 RED**

```powershell
rtk mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" "-Dtest=SearchHistoryServiceTest,SearchHistoryControllerTest" test
```

Expected: FAIL，因为接口尚不存在。

- [ ] **Step 3: 实现响应和服务**

```java
public record RecentSearchResponse(
        Long id,
        String ingredients,
        String mealType,
        String goal,
        String searchMode,
        LocalDateTime createdAt
) {
}
```

`SearchHistoryService.recent(Long userId)`：

```java
List<SearchLog> candidates = mapper.selectList(new QueryWrapper<SearchLog>()
        .eq("user_id", userId)
        .orderByDesc("created_at")
        .orderByDesc("id")
        .last("LIMIT 50"));

Set<String> seen = new LinkedHashSet<>();
return candidates.stream()
        .filter(log -> seen.add(dedupKey(log)))
        .limit(5)
        .map(this::toResponse)
        .toList();
```

去重键使用规范化后的 `queryText + mealType + goal`，空值按空字符串处理。

- [ ] **Step 4: 实现控制器**

```java
@RestController
@RequestMapping("/api/search-history")
public class SearchHistoryController {
    @GetMapping("/recent")
    public ApiResponse<List<RecentSearchResponse>> recent(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.recent(principal.id()));
    }
}
```

- [ ] **Step 5: 运行测试并确认 GREEN**

运行 Step 2 命令，Expected: PASS。

- [ ] **Step 6: 用户批准后提交本任务**

```powershell
rtk git add backend/src/main/java/com/example/food/recipe/SearchHistory* backend/src/main/java/com/example/food/recipe/dto/RecentSearchResponse.java backend/src/test/java/com/example/food/recipe/SearchHistory*
rtk git commit -m "feat: 增加最近搜索查询"
```

---

### Task 5: 我的菜谱筛选和删除后端

**Files:**

- Modify: `backend/src/main/java/com/example/food/recipe/RecipeRecordMapper.java`
- Modify: `backend/src/main/java/com/example/food/recipe/SavedRecipeService.java`
- Modify: `backend/src/main/java/com/example/food/recipe/SavedRecipeController.java`
- Modify: `backend/src/test/java/com/example/food/recipe/SavedRecipeServiceTest.java`
- Modify: `backend/src/test/java/com/example/food/recipe/SavedRecipeControllerTest.java`
- Modify: `backend/src/test/java/com/example/food/recipe/RecipePersistenceIntegrationTest.java`

- [ ] **Step 1: 写筛选和删除失败测试**

服务测试必须覆盖：

```java
@Test
void listsRecipesWithUserOwnedFilters() {
    service.list(7L, "番茄", "dinner", "balanced", 20, 0);

    verify(recipeRecordMapper).selectSavedByFilters(
            7L, "番茄", "dinner", "balanced", 20, 0
    );
}

@Test
void deletesOnlyCurrentUserRecipe() {
    RecipeRecord record = record(10L, 7L);
    when(recipeRecordMapper.selectById(10L)).thenReturn(record);

    service.delete(7L, 10L);

    verify(recipeRecordMapper).deleteById(10L);
    verify(searchLogMapper, never()).deleteById(anyLong());
}

@Test
void hidesOwnershipWhenDeletingAnotherUsersRecipe() {
    when(recipeRecordMapper.selectById(10L)).thenReturn(record(10L, 8L));

    assertThatThrownBy(() -> service.delete(7L, 10L))
            .hasMessageContaining("菜谱不存在");
}
```

- [ ] **Step 2: 运行聚焦测试并确认 RED**

```powershell
rtk mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" "-Dtest=SavedRecipeServiceTest,SavedRecipeControllerTest,RecipePersistenceIntegrationTest" test
```

Expected: FAIL，因为筛选查询和删除方法尚不存在。

- [ ] **Step 3: 增加 Mapper 关联筛选查询**

在 `RecipeRecordMapper` 增加：

```java
@Select("""
        <script>
        SELECT rr.*
        FROM recipe_records rr
        LEFT JOIN search_logs sl ON sl.id = rr.search_log_id
        WHERE rr.user_id = #{userId}
        <if test='keyword != null and keyword != ""'>
          AND rr.title LIKE CONCAT('%', #{keyword}, '%')
        </if>
        <if test='mealType != null and mealType != "" and mealType != "any"'>
          AND sl.meal_type = #{mealType}
        </if>
        <if test='goal != null and goal != "" and goal != "any"'>
          AND sl.goal = #{goal}
        </if>
        ORDER BY rr.created_at DESC, rr.id DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
List<RecipeRecord> selectSavedByFilters(
        @Param("userId") Long userId,
        @Param("keyword") String keyword,
        @Param("mealType") String mealType,
        @Param("goal") String goal,
        @Param("limit") int limit,
        @Param("offset") int offset
);
```

- [ ] **Step 4: 扩展服务和控制器**

服务签名：

```java
public List<RecipeHistorySummaryResponse> list(
        Long userId, String keyword, String mealType, String goal, int limit, int offset)

@Transactional
public void delete(Long userId, Long recipeId)
```

控制器：

```java
@GetMapping
public ApiResponse<List<RecipeHistorySummaryResponse>> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String mealType,
        @RequestParam(required = false) String goal,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset)

@DeleteMapping("/{id}")
public ApiResponse<Void> delete(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long id) {
    savedRecipeService.delete(principal.id(), id);
    return ApiResponse.ok(null);
}
```

保留旧 `list(userId, limit, offset)` 重载，降低现有调用和测试迁移风险。

- [ ] **Step 5: 增加真实数据库集成断言**

集成测试插入两个用户、不同搜索条件和菜谱，验证：

1. 组合筛选只返回当前用户匹配记录。
2. 删除后 `recipe_records`、`recipe_ingredients`、`recipe_steps` 对应行消失。
3. 关联 `search_logs` 仍存在。

- [ ] **Step 6: 运行聚焦测试并确认 GREEN**

运行 Step 2 命令，Expected: PASS。

- [ ] **Step 7: 用户批准后提交本任务**

```powershell
rtk git add backend/src/main/java/com/example/food/recipe backend/src/test/java/com/example/food/recipe
rtk git commit -m "feat: 增加个人菜谱筛选和删除"
```

---

### Task 6: 前端 API 与个人化纯函数

**Files:**

- Create: `frontend/src/api/userPreferences.js`
- Create: `frontend/src/api/searchHistory.js`
- Create: `frontend/src/utils/personalization.js`
- Create: `frontend/src/utils/personalization.test.js`
- Modify: `frontend/src/api/recipes.js`

- [ ] **Step 1: 写纯函数失败测试**

```javascript
test('normalizes diet preference and builds recipe payload', () => {
  assert.deepEqual(normalizeDietPreference({
    taste: 'light',
    defaultGoal: 'balanced',
    avoidIngredients: [' 香菜 ', '香菜', ''],
    allergenIngredients: ['花生']
  }), {
    taste: 'light',
    defaultGoal: 'balanced',
    avoidIngredients: ['香菜'],
    allergenIngredients: ['花生']
  })
})

test('maps recent search to workbench fields without generating', () => {
  assert.deepEqual(toSearchForm({
    ingredients: '番茄、鸡蛋',
    mealType: 'dinner',
    goal: 'balanced'
  }), {
    ingredients: '番茄、鸡蛋',
    mealType: 'dinner',
    goal: 'balanced'
  })
})
```

- [ ] **Step 2: 运行测试并确认 RED**

```powershell
rtk node --test src/utils/personalization.test.js
```

Expected: FAIL，因为工具尚不存在。

- [ ] **Step 3: 实现纯函数**

导出：

```javascript
export const EMPTY_DIET_PREFERENCE = Object.freeze({
  taste: 'any',
  defaultGoal: 'balanced',
  avoidIngredients: [],
  allergenIngredients: []
})

function normalizeIngredientList(items) {
  const seen = new Set()
  return (Array.isArray(items) ? items : [])
    .map((item) => String(item || '').trim())
    .filter((item) => {
      const key = item.toLocaleLowerCase()
      if (!item || seen.has(key)) {
        return false
      }
      seen.add(key)
      return true
    })
    .slice(0, 20)
}

export function normalizeDietPreference(value = {}) {
  return {
    taste: value.taste || EMPTY_DIET_PREFERENCE.taste,
    defaultGoal: value.defaultGoal || EMPTY_DIET_PREFERENCE.defaultGoal,
    avoidIngredients: normalizeIngredientList(value.avoidIngredients),
    allergenIngredients: normalizeIngredientList(value.allergenIngredients)
  }
}

export function toSearchForm(history = {}) {
  return {
    ingredients: String(history.ingredients || '').trim(),
    mealType: history.mealType || 'any',
    goal: history.goal || 'balanced'
  }
}

export function buildRecipeDietPreference(value) {
  const normalized = normalizeDietPreference(value)
  return {
    ...normalized,
    avoidIngredients: [...normalized.avoidIngredients],
    allergenIngredients: [...normalized.allergenIngredients]
  }
}
```

- [ ] **Step 4: 实现 API 文件**

```javascript
// userPreferences.js
export function getDietPreference() {
  return http.get('/users/me/diet-preferences')
}

export function saveDietPreference(payload) {
  return http.put('/users/me/diet-preferences', payload)
}

// searchHistory.js
export function getRecentSearches() {
  return http.get('/search-history/recent')
}
```

在 `recipes.js` 增加：

```javascript
export function deleteSavedRecipe(id) {
  return http.delete(`/recipes/saved/${id}`)
}
```

- [ ] **Step 5: 运行全部工具测试并确认 GREEN**

```powershell
rtk node --test src/utils/*.test.js
```

Expected: 全部 PASS。

- [ ] **Step 6: 用户批准后提交本任务**

```powershell
rtk git add frontend/src/api frontend/src/utils/personalization.js frontend/src/utils/personalization.test.js
rtk git commit -m "feat: 增加个人化前端数据层"
```

---

### Task 7: 工作台偏好弹层与最近搜索下拉

**Files:**

- Create: `frontend/src/components/DietPreferenceDialog.vue`
- Create: `frontend/src/components/RecentSearchPopover.vue`
- Modify: `frontend/src/views/HomeView.vue`

- [ ] **Step 1: 创建偏好弹层组件**

组件契约：

```javascript
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  preference: { type: Object, required: true },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])
```

界面使用：

- `el-dialog`，中文标题“饮食偏好”。
- 口味使用 `el-radio-group`。
- 默认目标使用 `el-select`。
- 忌口和过敏使用可创建多选 `el-select multiple filterable allow-create`。
- 保存按钮提交规范化副本，不直接修改父组件对象。

- [ ] **Step 2: 创建最近搜索组件**

组件契约：

```javascript
const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'update:visible'])
```

每项展示食材、餐次/目标中文标签和时间。列表最多渲染 5 条；空列表显示“暂无最近搜索”。

- [ ] **Step 3: 在 HomeView 接入状态与加载**

新增状态：

```javascript
const dietPreference = ref(normalizeDietPreference())
const preferenceDialogVisible = ref(false)
const preferenceSaving = ref(false)
const recentSearches = ref([])
const recentSearchLoading = ref(false)
const recentSearchVisible = ref(false)
```

仅 `auth.isUser` 时：

1. 页面挂载加载偏好。
2. 食材输入框首次聚焦请求最近搜索。
3. 登录角色变化时清空不属于当前用户的状态。

- [ ] **Step 4: 接入回填和保存**

```javascript
function applyRecentSearch(item) {
  const form = toSearchForm(item)
  ingredients.value = form.ingredients
  mealType.value = form.mealType || 'any'
  goal.value = form.goal || dietPreference.value.defaultGoal || 'balanced'
  recentSearchVisible.value = false
}

async function persistDietPreference(value) {
  preferenceSaving.value = true
  try {
    const response = await saveDietPreference(normalizeDietPreference(value))
    dietPreference.value = normalizeDietPreference(response.data.data)
    preferenceDialogVisible.value = false
    ElMessage.success('饮食偏好已保存')
  } finally {
    preferenceSaving.value = false
  }
}
```

- [ ] **Step 5: 生成和再生成携带偏好**

两个请求都增加：

```javascript
dietPreference: buildRecipeDietPreference(dietPreference.value)
```

偏好默认目标只在当前 `goal` 为空或为默认初始值时应用，不覆盖用户刚选择的目标。

- [ ] **Step 6: 自动运行前端测试和构建**

```powershell
rtk node --test src/utils/*.test.js
rtk npm.cmd run build
```

Expected: 全部 PASS，Vite build 成功。

- [ ] **Step 7: 浏览器检查**

在 `1280x720` 验证工作台无需新增页面滚动；检查：

1. 普通用户点击输入框显示最近 5 条。
2. 管理员和游客不显示历史下拉和偏好入口。
3. 偏好弹层不遮挡关键操作，中文文本无溢出。
4. 移动端无横向溢出。

测试结束后关闭临时 Vite/后端进程并确认端口释放。

- [ ] **Step 8: 用户批准后提交本任务**

```powershell
rtk git add frontend/src/components/DietPreferenceDialog.vue frontend/src/components/RecentSearchPopover.vue frontend/src/views/HomeView.vue
rtk git commit -m "feat: 增加饮食偏好和最近搜索交互"
```

---

### Task 8: 我的菜谱搜索、筛选与删除前端

**Files:**

- Modify: `frontend/src/views/SavedRecipesView.vue`
- Modify: `frontend/src/api/recipes.js`

- [ ] **Step 1: 增加筛选状态和参数查询**

```javascript
const filters = reactive({
  keyword: '',
  mealType: 'any',
  goal: 'any'
})

async function loadRecipes() {
  const response = await listSavedRecipes({
    keyword: filters.keyword.trim() || undefined,
    mealType: filters.mealType === 'any' ? undefined : filters.mealType,
    goal: filters.goal === 'any' ? undefined : filters.goal,
    limit: 50,
    offset: 0
  })
  recipes.value = response.data.data || []
}
```

搜索框使用回车和 300ms 防抖触发；筛选选择变化立即刷新。

- [ ] **Step 2: 增加删除交互**

使用 `Trash2` 图标按钮和 `ElMessageBox.confirm`：

```javascript
async function removeRecipe(recipe) {
  await ElMessageBox.confirm(
    `确认删除“${recipe.title}”吗？`,
    '删除菜谱',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteSavedRecipe(recipe.id)
  await loadRecipes()
  if (selected.value?.id === recipe.id) {
    selected.value = null
  }
  if (recipes.value.length) {
    await openRecipe(recipes.value[0].id)
  }
  ElMessage.success('菜谱已删除')
}
```

取消确认不显示错误提示；删除失败保留原状态。

- [ ] **Step 3: 保持现有布局**

- 筛选工具栏放在左侧列表标题下方。
- 删除按钮放在每个菜谱列表项右侧，阻止事件冒泡，避免删除确认时同时打开详情。
- 桌面端仍保持页面内滚动，不新增全页滚动。
- 移动端筛选控件换行但不横向溢出。

- [ ] **Step 4: 自动运行前端验证**

```powershell
rtk node --test src/utils/*.test.js
rtk npm.cmd run build
```

Expected: 全部 PASS，Vite build 成功。

- [ ] **Step 5: 浏览器检查并清理进程**

验证关键词、餐次、目标组合筛选；删除确认、取消和空列表状态；完成后关闭 agent 启动的全部服务并释放端口。

- [ ] **Step 6: 用户批准后提交本任务**

```powershell
rtk git add frontend/src/views/SavedRecipesView.vue frontend/src/api/recipes.js
rtk git commit -m "feat: 完善个人菜谱管理"
```

---

### Task 9: 集成验证、Review 与文档同步

**Files:**

- Modify if needed: `README.md`
- Review: all files changed by Tasks 1-8

- [ ] **Step 1: 运行后端完整测试**

```powershell
rtk mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" test
```

Expected: `Failures: 0, Errors: 0`。

- [ ] **Step 2: 运行前端完整验证**

```powershell
rtk node --test src/utils/*.test.js
rtk npm.cmd run build
```

Expected: 全部测试通过，生产构建成功。

- [ ] **Step 3: 执行安全与回归检查**

确认：

- 游客仍可使用文字和上传图片生成菜谱。
- 普通用户只能读取自己的偏好、搜索历史和菜谱。
- 管理员侧不显示个人入口，个人接口返回 403。
- 重新生成携带偏好并新增独立搜索记录。
- 最近搜索最多 5 条但数据库旧日志仍保留。
- 删除菜谱不删除 `search_logs`。
- 无 API Key、密码或本地配置进入差异。

- [ ] **Step 4: Review 差异**

```powershell
rtk git diff --check
rtk git status --short
rtk git diff --stat
```

逐项检查请求匹配、无关改动、编码、异常处理和测试缺口。修复 Critical/Important 发现后重新运行完整验证。

- [ ] **Step 5: 清理验证进程**

停止 agent 启动的前端、后端和浏览器辅助进程，确认 `5173` 与 `7068` 没有 agent 遗留监听。不得停止用户自行启动的进程。

- [ ] **Step 6: 报告结果并列出剩余功能**

剩余功能候选：拍照识别、烹饪步骤模式、成品图评价、真实短信、部署与 APK。推荐下一项为烹饪步骤模式或拍照识别，由用户选择。

- [ ] **Step 7: 用户明确批准后提交并推送**

```powershell
rtk git add <本轮已复核的功能文件>
rtk git commit -m "feat: 增加用户个性化与历史管理"
rtk git push origin main
```

不暂存 `FoodApplication.java` 的用户改动、IDE `.iml` 或其他无关未跟踪目录。
