# AI 菜谱增强功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成缺失食材与替代建议、菜谱再生成、全部食材采购清单和 AI 菜谱解释，并保持菜谱保存历史兼容。

**Architecture:** 后端扩展现有生成请求和响应契约，让千问一次返回完整菜谱、替代建议与解释；保存服务通过 `raw_response` 保留扩展数据。前端使用独立纯函数生成全部采购清单和安全编码的第三方搜索链接，并在现有左右切换窗口中增加三个页签。

**Tech Stack:** Java 17、Spring Boot 3、Jackson、MyBatis-Plus、Vue 3、Element Plus、Node test、Maven。

---

### Task 1: AI 生成契约与重新生成

**Files:**
- Modify: `backend/src/main/java/com/example/food/ai/recipe/dto/RecipeGenerateRequest.java`
- Modify: `backend/src/main/java/com/example/food/ai/recipe/dto/RecipeGenerateResponse.java`
- Modify: `backend/src/main/java/com/example/food/ai/recipe/RecipeRecommendationService.java`
- Modify: `backend/src/main/java/com/example/food/ai/qwen/QwenRecipeClient.java`
- Test: `backend/src/test/java/com/example/food/ai/recipe/RecipeRecommendationServiceTest.java`
- Test: `backend/src/test/java/com/example/food/ai/qwen/QwenRecipeClientTest.java`
- Test: `backend/src/test/java/com/example/food/ai/recipe/RecipeControllerTest.java`

- [ ] 先增加失败测试，断言扩展 JSON 可解析、空字段兼容、再生成方向进入提示词且产生新搜索记录。
- [ ] 运行聚焦 Maven 测试并确认因新契约尚未实现而失败。
- [ ] 扩展请求的 `regenerationPreference`、`previousTitle` 可选字段，并保留四参数构造器兼容现有代码。
- [ ] 扩展响应的 `missingIngredients` 和 `explanation`，所有集合及说明空值安全。
- [ ] 更新中文提示词和 Qwen 载荷解析。
- [ ] 再次运行聚焦测试并确认通过。

### Task 2: 保存与历史扩展数据

**Files:**
- Modify: `backend/src/main/java/com/example/food/recipe/dto/SaveRecipeRequest.java`
- Modify: `backend/src/main/java/com/example/food/recipe/SavedRecipeService.java`
- Test: `backend/src/test/java/com/example/food/recipe/SavedRecipeServiceTest.java`
- Test: `backend/src/test/java/com/example/food/recipe/RecipePersistenceIntegrationTest.java`

- [ ] 先增加失败测试，断言保存后详情包含替代建议与 AI 解释。
- [ ] 运行聚焦测试并确认失败原因是扩展数据未恢复。
- [ ] 保存请求接收扩展字段，`raw_response` 写入完整响应。
- [ ] 历史详情优先反序列化 `raw_response`，解析失败或旧记录缺字段时回退现有规范化表数据。
- [ ] 运行保存服务与持久化集成测试并确认通过。

### Task 3: 采购清单纯函数

**Files:**
- Create: `frontend/src/utils/recipeEnhancements.js`
- Create: `frontend/src/utils/recipeEnhancements.test.js`

- [ ] 先增加失败测试，覆盖中文分隔符、别名、全部食材、已有/待购状态和购买链接编码。
- [ ] 运行 `node --test src/utils/recipeEnhancements.test.js` 并确认模块缺失失败。
- [ ] 实现食材名称归一化、采购清单生成和京东/淘宝搜索链接。
- [ ] 再次运行 Node 测试并确认通过。

### Task 4: 首页扩展交互

**Files:**
- Modify: `frontend/src/api/recipes.js`
- Modify: `frontend/src/views/HomeView.vue`

- [ ] 接入扩展请求字段和再生成方向。
- [ ] 在菜谱标题操作区增加重新生成菜单，并在请求失败时保留当前菜谱。
- [ ] 在现有菜谱分页窗口增加“食材分析”“采购清单”“AI 解释”。
- [ ] 保存请求包含扩展数据。
- [ ] 检查中文文案、键盘焦点、外链安全属性和固定高度布局。

### Task 5: 历史页扩展展示

**Files:**
- Modify: `frontend/src/views/SavedRecipesView.vue`

- [ ] 复用采购清单纯函数，根据 `searchIngredients` 和已保存菜谱生成清单。
- [ ] 增加“食材分析”“采购清单”“AI 解释”页签。
- [ ] 对旧记录缺少扩展字段提供空状态且不影响原有食材、步骤、技巧和视频关键词页签。

### Task 6: 集成验证与 Review

**Files:**
- Review all files changed by Tasks 1-5.

- [ ] 运行后端全量测试：`mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" test`。
- [ ] 运行前端测试：`node --test src/utils/*.test.js`。
- [ ] 运行前端构建：`npm run build`。
- [ ] 启动临时服务完成桌面和移动浏览器检查，再停止本轮启动的所有进程并确认端口释放。
- [ ] 检查 diff、API Key、编码、无关改动与未提交用户文件。

