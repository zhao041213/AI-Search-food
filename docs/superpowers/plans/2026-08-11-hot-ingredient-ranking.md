# 热门食材图片排行榜 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从成功菜谱搜索中形成可持久化、可按时间筛选的全局热门食材排行榜，并在用户端和管理员端分别提供图片榜单与分析视图。

**Architecture:** Flyway 新增规范化搜索食材明细表；`SearchLogService` 在原事务内写入明细，启动回填器补齐历史数据；统计服务通过 MyBatis 聚合查询生成公开响应。Vue 用户页面显示本地图片卡片，管理员组件使用 ECharts 和 Element Plus 表格展示同一接口的数据。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、Flyway、JUnit 5、Mockito、Vue 3、Element Plus、ECharts、Vite。

---

### Task 1: 数据结构与食材归一化

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__add_search_log_ingredients.sql`
- Create: `backend/src/main/java/com/example/food/stats/IngredientNormalizer.java`
- Create: `backend/src/test/java/com/example/food/stats/IngredientNormalizerTest.java`
- Modify: `backend/src/test/java/com/example/food/recipe/PersistenceSchemaTest.java`

- [ ] 写失败测试：验证多分隔符、`西红柿/番茄` 和 `马铃薯/土豆` 合并、保持原始名称及标准名去重。
- [ ] 运行 `mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -Dtest=IngredientNormalizerTest,PersistenceSchemaTest test`，预期因类和表不存在而失败。
- [ ] 实现 `IngredientNormalizer.normalizeDistinct(String)`，返回 `originalName` 与 `canonicalName` 记录列表。
- [ ] 新增 V7 迁移，创建明细表、外键、唯一键和联合索引。
- [ ] 重跑聚焦测试，预期通过。

### Task 2: 新搜索明细写入与历史回填

**Files:**
- Create: `backend/src/main/java/com/example/food/stats/SearchLogIngredient.java`
- Create: `backend/src/main/java/com/example/food/stats/SearchLogIngredientMapper.java`
- Create: `backend/src/main/java/com/example/food/stats/SearchLogIngredientBackfill.java`
- Modify: `backend/src/main/java/com/example/food/recipe/SearchLogMapper.java`
- Modify: `backend/src/main/java/com/example/food/recipe/SearchLogService.java`
- Modify: `backend/src/test/java/com/example/food/recipe/SearchLogServiceTest.java`
- Create: `backend/src/test/java/com/example/food/stats/SearchLogIngredientBackfillTest.java`

- [ ] 扩展失败测试：保存搜索时应为每个标准食材写一条明细，重复别名只写一次，并沿用搜索时间。
- [ ] 新增失败测试：回填器解析历史 JSON、重复运行不重复插入、坏 JSON 仅跳过当前记录。
- [ ] 运行两个测试类，确认因依赖和行为缺失而失败。
- [ ] 实现实体、Mapper 和 `SearchLogService` 事务内明细写入。
- [ ] 实现启动回填器，通过“没有任何明细的搜索日志”查询和数据库唯一键保证幂等。
- [ ] 重跑两个测试类，预期通过。

### Task 3: 排行榜聚合 API

**Files:**
- Create: `backend/src/main/java/com/example/food/stats/HotIngredientPeriod.java`
- Create: `backend/src/main/java/com/example/food/stats/HotIngredientAggregateRow.java`
- Create: `backend/src/main/java/com/example/food/stats/HotIngredientTotals.java`
- Create: `backend/src/main/java/com/example/food/stats/dto/HotIngredientItemResponse.java`
- Create: `backend/src/main/java/com/example/food/stats/dto/HotIngredientStatsResponse.java`
- Create: `backend/src/main/java/com/example/food/stats/HotIngredientStatsService.java`
- Create: `backend/src/main/java/com/example/food/stats/HotIngredientStatsController.java`
- Create: `backend/src/test/java/com/example/food/stats/HotIngredientStatsServiceTest.java`
- Create: `backend/src/test/java/com/example/food/stats/HotIngredientStatsControllerTest.java`
- Modify: `backend/src/test/java/com/example/food/security/SecurityConfigTest.java`

- [ ] 写失败测试：`all/7d/30d` 截止时间、排名、次数、占比和最近时间映射正确。
- [ ] 写失败测试：接口默认参数、公开访问、非法 period 和越界 limit 返回 400。
- [ ] 运行统计测试，确认因统计类和接口不存在而失败。
- [ ] 在 Mapper 增加总数及排行聚合 SQL；实现 period 解析、limit 验证、占比计算和 DTO。
- [ ] 实现 `GET /api/stats/hot-ingredients` 控制器。
- [ ] 重跑统计和安全测试，预期通过。

### Task 4: 用户端图片排行榜

**Files:**
- Create: `frontend/src/api/stats.js`
- Create: `frontend/src/utils/ingredientImages.js`
- Create: `frontend/src/views/HotIngredientsView.vue`
- Create: `frontend/public/images/ingredients/*`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`

- [ ] 创建本地常见食材图片和默认回退图，文件名使用稳定英文标识。
- [ ] 新增统计 API 封装和食材名到本地图片的映射。
- [ ] 实现全部、7 天、30 天切换和前 10 名图片卡片的加载、空数据、错误状态。
- [ ] 将首个“功能扩展”占位按钮替换为“热门食材”路由入口。
- [ ] 点击卡片跳转首页并通过查询参数预填食材；首页只预填，不自动生成。
- [ ] 运行 `npm run build`，预期构建通过。

### Task 5: 管理员统计视图

**Files:**
- Create: `frontend/src/components/AdminHotIngredientsPanel.vue`
- Modify: `frontend/src/views/AdminDashboardView.vue`

- [ ] 在管理员后台增加“系统设置/热门食材”页签。
- [ ] 实现前 10 横向柱状图、前 20 明细表、时间筛选和汇总指标。
- [ ] 为图表增加容器尺寸监听、卸载释放和空数据处理。
- [ ] 运行 `npm run build`，预期构建通过且无新增警告。

### Task 6: 集成验证与 Review

**Files:**
- Modify as needed only for defects found in the files above.

- [ ] 运行 `mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" test`，确认全部后端测试通过。
- [ ] 运行 `npm run build`，确认前端生产构建通过。
- [ ] 启动前后端，验证公开接口、用户排行榜、卡片跳转预填和管理员图表。
- [ ] 使用桌面与移动视口检查中文文本、图片、图表、滚动和遮挡。
- [ ] Review `git diff`，确认未改动 `FoodApplication.java`、IDEA 文件或无关模块，并检查密钥、编码与迁移兼容性。
- [ ] 向用户汇报剩余功能并等待明确授权后再提交、推送。
