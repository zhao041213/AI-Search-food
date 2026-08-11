# 搜索记录与个人菜谱保存 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 AI 菜谱生成和图片识别基础上，自动记录搜索行为，并让登录用户通过保存按钮把菜谱写入自己的历史列表。

**Architecture:** 生成请求继续公开访问，AI 成功后由后端写入 `search_logs`，响应附带 `searchLogId`。菜谱只有在登录用户点击保存后才由后端以 JWT 用户 ID 写入 `recipe_records`、`recipe_ingredients` 和 `recipe_steps`；历史接口只按当前用户查询。前端在 App 全局布局增加 A 方案文字侧栏，Home 保留文字输入、上传识别和拍照占位，新增个人菜谱页面。

**Tech Stack:** Java 17, Spring Boot 3, Spring Security JWT, MyBatis-Plus, Flyway, H2/MySQL, Vue 3, Vue Router, Pinia, Element Plus, Axios, Vite.

---

## Scope Guard

本计划只实现搜索记录、主动保存、个人历史和左侧导航。本轮不实现真实拍照、热门食材图表、收藏删除、菜谱再生成、采购链接、真实视频搜索、成品评价和管理员数据看板。

## File Map

Backend files:

- Create `backend/src/main/resources/db/migration/V6__add_recipe_history_fields.sql`: 为搜索条件和菜谱摘要补充数据库字段。
- Create `backend/src/main/java/com/example/food/recipe/SearchLog.java`: `search_logs` 表实体。
- Create `backend/src/main/java/com/example/food/recipe/SearchLogMapper.java`: 搜索记录基础 Mapper。
- Create `backend/src/main/java/com/example/food/recipe/RecipeRecord.java`: `recipe_records` 表实体。
- Create `backend/src/main/java/com/example/food/recipe/RecipeIngredient.java`: `recipe_ingredients` 表实体。
- Create `backend/src/main/java/com/example/food/recipe/RecipeStep.java`: `recipe_steps` 表实体。
- Create `backend/src/main/java/com/example/food/recipe/RecipeRecordMapper.java`: 菜谱主体 Mapper。
- Create `backend/src/main/java/com/example/food/recipe/RecipeIngredientMapper.java`: 菜谱食材 Mapper。
- Create `backend/src/main/java/com/example/food/recipe/RecipeStepMapper.java`: 菜谱步骤 Mapper。
- Create `backend/src/main/java/com/example/food/recipe/dto/SaveRecipeRequest.java`: 保存菜谱请求体及校验。
- Create `backend/src/main/java/com/example/food/recipe/dto/RecipeHistorySummaryResponse.java`: 历史列表摘要。
- Create `backend/src/main/java/com/example/food/recipe/dto/RecipeHistoryDetailResponse.java`: 历史详情响应。
- Create `backend/src/main/java/com/example/food/recipe/SearchLogService.java`: 生成成功后的搜索记录保存。
- Create `backend/src/main/java/com/example/food/recipe/SavedRecipeService.java`: 菜谱保存、列表和详情查询。
- Create `backend/src/main/java/com/example/food/recipe/SavedRecipeController.java`: `/api/recipes/saved` 接口。
- Modify `backend/src/main/java/com/example/food/ai/recipe/dto/RecipeGenerateResponse.java`: 增加 `searchLogId`，并保留旧构造方法兼容现有测试。
- Modify `backend/src/main/java/com/example/food/ai/recipe/RecipeRecommendationService.java`: AI 成功后调用搜索记录服务，并传递用户和匿名标识。
- Modify `backend/src/main/java/com/example/food/ai/recipe/RecipeController.java`: 接收可选 JWT 用户和 `X-Anonymous-Id` 请求头。
- Modify `backend/src/main/java/com/example/food/security/SecurityConfig.java`: 明确个人菜谱接口只允许 USER 角色。

Backend tests:

- Create `backend/src/test/java/com/example/food/recipe/SearchLogServiceTest.java`.
- Create `backend/src/test/java/com/example/food/recipe/SavedRecipeServiceTest.java`.
- Create `backend/src/test/java/com/example/food/recipe/SavedRecipeControllerTest.java`.
- Modify `backend/src/test/java/com/example/food/ai/recipe/RecipeControllerTest.java` only for the new response metadata and optional header behavior.
- Modify `backend/src/test/java/com/example/food/auth/AuthSecurityTest.java` for saved-recipe role protection.

Frontend files:

- Create `frontend/src/utils/anonymousId.js`: 生成并读取浏览器匿名 ID。
- Create `frontend/src/views/SavedRecipesView.vue`: 当前用户的我的菜谱列表和详情。
- Modify `frontend/src/api/http.js`: 为请求提供匿名 ID，并保留现有 JWT 注入和 401 处理。
- Modify `frontend/src/api/recipes.js`: 增加保存、列表和详情请求函数。
- Modify `frontend/src/router/index.js`: 增加 `/recipes/saved`，并增加 USER 登录守卫。
- Modify `frontend/src/App.vue`: 增加 A 方案左侧文字导航和占位按钮。
- Modify `frontend/src/views/HomeView.vue`: 增加保存按钮、保存状态、游客登录提示和拍照识别占位，保留上传识别闭环。

---

### Task 1: Add database migration and persistence entities

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__add_recipe_history_fields.sql`
- Create: `backend/src/main/java/com/example/food/recipe/SearchLog.java`
- Create: `backend/src/main/java/com/example/food/recipe/SearchLogMapper.java`
- Create: `backend/src/main/java/com/example/food/recipe/RecipeRecord.java`
- Create: `backend/src/main/java/com/example/food/recipe/RecipeIngredient.java`
- Create: `backend/src/main/java/com/example/food/recipe/RecipeStep.java`
- Create: `backend/src/main/java/com/example/food/recipe/RecipeRecordMapper.java`
- Create: `backend/src/main/java/com/example/food/recipe/RecipeIngredientMapper.java`
- Create: `backend/src/main/java/com/example/food/recipe/RecipeStepMapper.java`
- Test: `backend/src/test/java/com/example/food/recipe/PersistenceSchemaTest.java`

- [ ] **Step 1: Write the failing schema test**

Add a MySQL-mode H2 integration test that queries `information_schema.columns` and asserts `search_logs.meal_type`, `search_logs.goal`, `recipe_records.summary`, `recipe_records.effects`, and `recipe_records.tips` exist after Flyway migration.

- [ ] **Step 2: Run the schema test and verify it fails for the missing V6 columns**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" -Dtest=PersistenceSchemaTest test
```

Expected: the test fails because the five columns are not present in the current schema.

- [ ] **Step 3: Add the V6 migration**

Create the migration with these statements:

```sql
ALTER TABLE search_logs
    ADD COLUMN meal_type VARCHAR(64),
    ADD COLUMN goal VARCHAR(128);

ALTER TABLE recipe_records
    ADD COLUMN summary TEXT,
    ADD COLUMN effects TEXT,
    ADD COLUMN tips TEXT;
```

- [ ] **Step 4: Add MyBatis-Plus entities and BaseMapper interfaces**

Map snake-case columns through the existing `map-underscore-to-camel-case` setting. Include only fields used by this feature: IDs, ownership, search metadata, recipe summary data, ingredients, steps, timestamps, and raw response. Use `@TableId(type = IdType.AUTO)` and `@TableName` consistent with `User`.

- [ ] **Step 5: Run the schema test and the existing test suite**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" -Dtest=PersistenceSchemaTest test
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" test
```

Expected: both commands pass and Flyway reports schema version 6.

### Task 2: Persist successful generation as a search log

**Files:**
- Create: `backend/src/main/java/com/example/food/recipe/SearchLogService.java`
- Modify: `backend/src/main/java/com/example/food/ai/recipe/dto/RecipeGenerateResponse.java`
- Modify: `backend/src/main/java/com/example/food/ai/recipe/RecipeRecommendationService.java`
- Modify: `backend/src/main/java/com/example/food/ai/recipe/RecipeController.java`
- Test: `backend/src/test/java/com/example/food/recipe/SearchLogServiceTest.java`
- Test: `backend/src/test/java/com/example/food/ai/recipe/RecipeRecommendationServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

Cover these behaviors:

```java
@Test
void recordsSuccessfulTextSearchForAuthenticatedUser() { }

@Test
void recordsAnonymousImageSearchWithAnonymousId() { }

@Test
void doesNotRecordWhenAiGenerationThrows() { }
```

The tests must assert the inserted `SearchLog` fields and that the returned response contains the generated search-log ID.

- [ ] **Step 2: Run the tests and verify the expected failures**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" -Dtest=SearchLogServiceTest,RecipeRecommendationServiceTest test
```

Expected: compilation or assertion failures because the service and response metadata do not exist.

- [ ] **Step 3: Extend the response without breaking current constructors**

Add `Long searchLogId` as the final component of `RecipeGenerateResponse`. Keep a nine-argument convenience constructor that delegates to the new canonical constructor with `null`, so existing Qwen client tests and controller fixtures continue to compile.

- [ ] **Step 4: Implement `SearchLogService`**

Use `@Transactional` only for the insert. The service must accept `RecipeGenerateRequest`, generated response, optional `AuthPrincipal`, and the `X-Anonymous-Id` value. Store `input_type` from `searchMode`, `query_text` from normalized ingredients, `recognized_ingredients` as a Jackson JSON array, `ai_model` as provider plus model, and request meal/goal values.

- [ ] **Step 5: Connect the generation service and controller**

Keep the AI call first. After it returns, call `SearchLogService`, then return a copy of the response with the inserted ID. Read an optional authenticated principal from the request; public callers remain valid. Trim and length-limit anonymous IDs before persistence.

- [ ] **Step 6: Run focused and full backend tests**

Run the two focused test classes, then the full Maven suite. Expected: generation tests pass, and an AI exception produces no mapper insert.

### Task 3: Implement authenticated recipe saving and history APIs

**Files:**
- Create: `backend/src/main/java/com/example/food/recipe/dto/SaveRecipeRequest.java`
- Create: `backend/src/main/java/com/example/food/recipe/dto/RecipeHistorySummaryResponse.java`
- Create: `backend/src/main/java/com/example/food/recipe/dto/RecipeHistoryDetailResponse.java`
- Create: `backend/src/main/java/com/example/food/recipe/SavedRecipeService.java`
- Create: `backend/src/main/java/com/example/food/recipe/SavedRecipeController.java`
- Test: `backend/src/test/java/com/example/food/recipe/SavedRecipeServiceTest.java`
- Test: `backend/src/test/java/com/example/food/recipe/SavedRecipeControllerTest.java`

- [ ] **Step 1: Write failing DTO and service tests**

The save request must require `searchLogId`, a nonblank title, at least one ingredient, and at least one step. Service tests must verify that the authenticated principal ID is used, the search-log ownership check rejects another user's log, and a list query only returns the current user's rows in descending time order.

- [ ] **Step 2: Run focused tests and verify they fail for missing classes**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" -Dtest=SavedRecipeServiceTest,SavedRecipeControllerTest test
```

Expected: failures because the save service, DTOs, and endpoints are not present.

- [ ] **Step 3: Implement the save transaction**

`SavedRecipeService.save` must:

1. Load `SearchLog` by `searchLogId`.
2. Reject a log bound to a different user; permit a null owner only when the submitted anonymous ID matches.
3. Insert `RecipeRecord` with current user ID, search-log ID, title, summary, effects JSON, tips JSON, video keywords JSON, AI model, and serialized raw response.
4. Insert every ingredient into `recipe_ingredients`.
5. Insert every step into `recipe_steps`, using the supplied order and duration.
6. Return the saved ID and full response.

Annotate the method with `@Transactional` so child inserts roll back with the parent.

- [ ] **Step 4: Implement history list and detail**

Use a bounded `limit` of 1-50 and a nonnegative `offset`. Query `recipe_records` by `user_id`, order by `created_at DESC`, and join or load the related search log to build summaries. For details, load the record, verify the principal ID, load ingredients and steps, deserialize arrays and `raw_response`, then return the existing recipe shape with saved metadata.

- [ ] **Step 5: Implement controller and security rules**

Expose:

```text
POST /api/recipes/saved
GET  /api/recipes/saved?limit=20&offset=0
GET  /api/recipes/saved/{id}
```

Use `@AuthenticationPrincipal AuthPrincipal` on protected methods. Add `/api/recipes/saved/**` as `hasRole("USER")`; admin access is not included in this user-history feature. Preserve the existing JSON error envelope.

- [ ] **Step 6: Run focused, security, and full backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" -Dtest=SavedRecipeServiceTest,SavedRecipeControllerTest,AuthSecurityTest test
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" test
```

Expected: unauthenticated save/list/detail requests return 401, another user's detail returns 403, and authorized requests return code 0.

### Task 4: Add frontend anonymous identity and recipe APIs

**Files:**
- Create: `frontend/src/utils/anonymousId.js`
- Modify: `frontend/src/api/http.js`
- Modify: `frontend/src/api/recipes.js`

- [ ] **Step 1: Add the anonymous ID utility**

Export `getAnonymousId()` that reads `ai_smart_recipe_anonymous_id` from `localStorage`, creates a UUID with `crypto.randomUUID()` when absent, stores it, and returns it. Do not use it as an authentication token.

- [ ] **Step 2: Add API functions**

Add functions that call the three saved-recipe endpoints:

```js
export function saveRecipe(payload) {
  return http.post('/recipes/saved', payload)
}

export function listSavedRecipes(params) {
  return http.get('/recipes/saved', { params })
}

export function getSavedRecipe(id) {
  return http.get(`/recipes/saved/${id}`)
}
```

Add `X-Anonymous-Id` to the existing request interceptor and keep the current Authorization behavior unchanged.

- [ ] **Step 3: Run the frontend production build**

Run:

```powershell
npm run build
```

Expected: Vite builds successfully before view changes begin.

### Task 5: Add A-scheme sidebar, save action, and personal history view

**Files:**
- Create: `frontend/src/views/SavedRecipesView.vue`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/LoginView.vue` only if redirect restoration needs a focused change

- [ ] **Step 1: Add the protected route and layout navigation**

Register `/recipes/saved` with `requiresUser: true`. Extend the router guard to redirect unauthenticated users to `/login?redirect=/recipes/saved`, while keeping the existing admin guard. Add a fixed desktop sidebar in `App.vue` with:

```text
智能工作台 -> /
我的菜谱   -> /recipes/saved
三个禁用占位按钮
```

Use existing `lucide-vue-next` icons and keep the current theme variables. Do not remove the admin entry or theme selector.

- [ ] **Step 2: Write the save interaction in HomeView**

Add a save button beside the generated recipe title. Track `savingRecipe`, `savedRecipeId`, and `pendingLoginRedirect`. On click:

1. Show a login message and route to `/login` with the current route as redirect if logged out.
2. Send the current recipe plus `searchLogId` to `saveRecipe` if logged in.
3. Disable duplicate clicks while saving.
4. Switch the button to `已保存` only after a successful response.
5. Keep the existing image upload, recognition button, confirmed ingredients, and unimplemented camera placeholder intact.

Store an unsaved guest result temporarily in `sessionStorage` only to restore the current screen after login; it must be removed after successful save and must never appear in the history list without a save request.

- [ ] **Step 3: Implement SavedRecipesView**

Load the current user's summaries on mount. Render a compact history list with title, ingredients, meal type, goal, and timestamp. Clicking an item loads its detail into the existing stacked recipe presentation: ingredients, steps, tips, effects, and video keywords. Show loading, empty, and error states in Chinese. Do not add nested decorative cards or an unrelated dashboard.

- [ ] **Step 4: Verify frontend behavior manually and build**

With backend on port 7068 and Vite on port 5173, verify:

1. Text generation still works.
2. Upload recognition still fills ingredients and then generates a recipe.
3. Camera placeholder is visible and does not send a request.
4. A guest cannot save and is redirected to login.
5. A logged-in user can save once and sees `已保存`.
6. The left `我的菜谱` entry opens only that user's saved records.
7. A second user cannot see the first user's saved recipe.

Run:

```powershell
npm run build
```

### Task 6: Final verification and review checkpoint

**Files:**
- Review all files changed by Tasks 1-5.

- [ ] **Step 1: Run the complete verification set**

```powershell
mvn "-Dmaven.repo.local=D:\\AI-Search-food\\.m2" test
npm run build
```

- [ ] **Step 2: Review the diff**

Check that only the migration, recipe persistence domain, relevant tests, API files, router, App layout, Home view, and saved-recipe view changed. Do not modify the existing unrelated `FoodApplication.java` blank line or untracked IDE module file.

- [ ] **Step 3: Review behavior and residual risk**

Confirm ownership checks use server-side JWT identity, anonymous IDs are not treated as credentials, failed AI responses do not create search logs, and the save transaction cannot leave orphaned ingredients or steps. Record any remaining gap: real camera capture is intentionally not implemented.

- [ ] **Step 4: Stop before Git operations and ask for approval**

Report tests, build, diff review, and residual risks. Do not commit or push until the user explicitly approves the GitHub operation.

---

## Plan Self-Review

- Spec coverage: search logging, manual save, per-user ownership, anonymous tracking, upload recognition, camera placeholder, A sidebar, history list/detail, tests, and exclusions are mapped to Tasks 1-6.
- Placeholder scan: no implementation step relies on an unspecified class, endpoint, migration, or test command.
- Type consistency: `searchLogId` is added to the existing recipe response; save payload uses the same ingredient and step field names already rendered by HomeView; history detail returns the same recipe shape.
- Scope check: all changes serve the approved feature. Favorite, shopping, video, and camera work remain explicitly excluded.
