<template>
  <main class="home-page">
    <section class="command-shell" aria-labelledby="home-title">
      <header class="workspace-heading">
        <div>
          <p class="eyebrow">AI 菜谱指挥舱</p>
          <h1 id="home-title">AI Ingredient Intelligence Workbench</h1>
        </div>
        <div class="signal-strip" aria-label="当前推荐信号">
          <span>{{ searchModeLabel }}</span>
          <span>{{ mealTypeLabel }}</span>
          <span>{{ goalLabel }}</span>
        </div>
      </header>

      <div class="command-grid">
        <section class="search-panel" aria-label="菜谱搜索表单">
          <div class="panel-title">
            <ScanSearch :size="20" aria-hidden="true" />
            <div>
              <span>输入信号</span>
              <strong>食材识别与推荐参数</strong>
            </div>
          </div>

          <el-form class="search-form" label-position="top" @submit.prevent="runSearch">
            <el-form-item label="食材清单">
              <el-input
                v-model="ingredients"
                type="textarea"
                :rows="3"
                resize="none"
                maxlength="240"
                show-word-limit
                placeholder="例如：番茄、鸡蛋、菠菜"
              />
            </el-form-item>

            <div class="filters">
              <el-form-item label="餐次">
                <el-select v-model="mealType" placeholder="请选择餐次">
                  <el-option label="不限餐次" value="any" />
                  <el-option label="早餐" value="breakfast" />
                  <el-option label="午餐" value="lunch" />
                  <el-option label="晚餐" value="dinner" />
                </el-select>
              </el-form-item>

              <el-form-item label="目标">
                <el-select v-model="goal" placeholder="请选择烹饪目标">
                  <el-option label="营养均衡" value="balanced" />
                  <el-option label="高蛋白" value="protein" />
                  <el-option label="低热量" value="light" />
                  <el-option label="快速烹饪" value="quick" />
                </el-select>
              </el-form-item>
            </div>

            <div class="mode-panel" aria-label="搜索方式">
              <span class="field-label">搜索方式</span>
              <div class="mode-grid">
                <button
                  v-for="mode in modeCards"
                  :key="mode.value"
                  class="mode-card"
                  :class="{ active: searchMode === mode.value }"
                  type="button"
                  :aria-pressed="searchMode === mode.value"
                  @click="searchMode = mode.value"
                >
                  <component :is="mode.icon" :size="18" aria-hidden="true" />
                  <strong>{{ mode.label }}</strong>
                  <span>{{ mode.description }}</span>
                </button>
              </div>
            </div>

            <div class="search-actions">
              <el-button type="primary" size="large" :loading="generating" @click="runSearch">
                <Sparkles :size="18" aria-hidden="true" />
                <span>生成推荐</span>
              </el-button>
              <el-button size="large" plain @click="resetSearch">
                <RotateCcw :size="18" aria-hidden="true" />
                <span>重置</span>
              </el-button>
            </div>
          </el-form>
        </section>

        <section class="result-panel" v-loading="generating" aria-label="菜谱搜索结果">
          <div class="result-header">
            <div>
              <p class="eyebrow">菜谱输出窗口</p>
              <h2>{{ resultTitle }}</h2>
            </div>
            <span class="status-pill">{{ generating ? '生成中' : searchModeLabel }}</span>
          </div>

          <div v-if="!hasSearch" class="empty-stage">
            <ChefHat :size="44" aria-hidden="true" />
            <strong>等待食材信号</strong>
            <span>输入食材后，系统会在右侧生成菜谱、功效、步骤和视频关键词。</span>
          </div>

          <div v-else class="result-content">
            <dl class="brief-grid">
              <div>
                <dt>食材</dt>
                <dd>{{ cleanIngredients }}</dd>
              </div>
              <div>
                <dt>餐次</dt>
                <dd>{{ mealTypeLabel }}</dd>
              </div>
              <div>
                <dt>目标</dt>
                <dd>{{ goalLabel }}</dd>
              </div>
            </dl>

            <div v-if="recipe" class="recipe-detail">
              <div class="recipe-summary-block">
                <p>{{ recipe.summary }}</p>
                <div v-if="recipe.effects?.length" class="tag-row" aria-label="菜谱功效">
                  <span v-for="effect in recipe.effects" :key="effect" class="system-tag">
                    {{ effect }}
                  </span>
                </div>
              </div>

              <div class="recipe-pages" aria-label="菜谱详情分页">
                <div class="page-toolbar">
                  <button
                    class="page-arrow"
                    type="button"
                    aria-label="上一页"
                    :disabled="recipePages.length <= 1"
                    @click="previousRecipePage"
                  >
                    <ChevronLeft :size="19" aria-hidden="true" />
                  </button>

                  <div class="page-tabs" role="tablist" aria-label="菜谱页面">
                    <button
                      v-for="(page, index) in recipePages"
                      :key="page.key"
                      class="page-tab"
                      :class="{ active: index === activeRecipePageIndex }"
                      type="button"
                      role="tab"
                      :aria-selected="index === activeRecipePageIndex"
                      @click="currentRecipePage = index"
                    >
                      {{ page.label }}
                    </button>
                  </div>

                  <button
                    class="page-arrow"
                    type="button"
                    aria-label="下一页"
                    :disabled="recipePages.length <= 1"
                    @click="nextRecipePage"
                  >
                    <ChevronRight :size="19" aria-hidden="true" />
                  </button>
                </div>

                <section
                  v-if="activeRecipePage"
                  class="recipe-page-window"
                  :aria-labelledby="`recipe-page-${activeRecipePage.key}`"
                >
                  <div class="window-head">
                    <h3 :id="`recipe-page-${activeRecipePage.key}`">{{ activeRecipePage.label }}</h3>
                    <span>{{ activeRecipePageIndex + 1 }} / {{ recipePages.length || 1 }}</span>
                  </div>

                  <el-table v-if="activeRecipePage.key === 'ingredients'" :data="recipe.ingredients" size="large">
                    <el-table-column prop="name" label="食材" min-width="120" />
                    <el-table-column prop="amount" label="用量" min-width="120" />
                  </el-table>

                  <ol v-else-if="activeRecipePage.key === 'steps'" class="step-list">
                    <li v-for="step in recipe.steps" :key="step.order || step.title">
                      <strong>{{ step.title }}</strong>
                      <span v-if="step.durationMinutes">约 {{ step.durationMinutes }} 分钟</span>
                      <p>{{ step.description }}</p>
                    </li>
                  </ol>

                  <ul v-else-if="activeRecipePage.key === 'tips'" class="tip-list">
                    <li v-for="tip in recipe.tips" :key="tip">{{ tip }}</li>
                  </ul>

                  <div v-else-if="activeRecipePage.key === 'videos'" class="video-keywords">
                    <span v-for="keyword in recipe.videoKeywords" :key="keyword">
                      <Video :size="15" aria-hidden="true" />
                      {{ keyword }}
                    </span>
                  </div>
                </section>

                <el-empty v-else description="暂无菜谱详情" />
              </div>
            </div>

            <div v-else class="next-steps">
              <div class="window-head">
                <h3>推荐生成队列</h3>
                <span>就绪</span>
              </div>
              <el-table :data="recommendationRows" size="large">
                <el-table-column prop="name" label="关注点" min-width="140" />
                <el-table-column prop="value" label="信号" min-width="140" />
              </el-table>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, markRaw, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Camera,
  ChefHat,
  ChevronLeft,
  ChevronRight,
  ImagePlus,
  RotateCcw,
  ScanSearch,
  Sparkles,
  Video
} from 'lucide-vue-next'
import { generateRecipe } from '../api/recipes'

const ingredients = ref('')
const mealType = ref('any')
const goal = ref('balanced')
const searchMode = ref('text')
const lastSearch = ref(null)
const recipe = ref(null)
const generating = ref(false)
const currentRecipePage = ref(0)

const modeCards = [
  { value: 'text', label: '文字输入', description: '手动输入现有食材', icon: markRaw(ScanSearch) },
  { value: 'image', label: '图片识别', description: '上传或拍照识别食材', icon: markRaw(ImagePlus) },
  { value: 'multi', label: '多模态', description: '文本与图像联合分析', icon: markRaw(Camera) }
]

const mealLabels = {
  any: '不限餐次',
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐'
}

const goalLabels = {
  balanced: '营养均衡',
  protein: '高蛋白',
  light: '低热量',
  quick: '快速烹饪'
}

const modeLabels = {
  text: '文字输入',
  image: '图片识别',
  multi: '多模态'
}

const hasSearch = computed(() => Boolean(lastSearch.value))
const cleanIngredients = computed(() => lastSearch.value?.ingredients || '暂无')
const mealTypeLabel = computed(() => mealLabels[lastSearch.value?.mealType || mealType.value] || mealLabels.any)
const goalLabel = computed(() => goalLabels[lastSearch.value?.goal || goal.value] || goalLabels.balanced)
const searchModeLabel = computed(() => modeLabels[lastSearch.value?.searchMode || searchMode.value])
const resultTitle = computed(() => {
  if (recipe.value?.title) {
    return recipe.value.title
  }
  return hasSearch.value ? '菜谱匹配简报' : '等待搜索'
})
const recommendationRows = computed(() => [
  { name: '营养目标', value: goalLabel.value },
  { name: '用餐时间', value: mealTypeLabel.value },
  { name: '输入方式', value: modeLabels[lastSearch.value?.searchMode] || modeLabels.text }
])
const recipePages = computed(() => {
  if (!recipe.value) {
    return []
  }

  return [
    recipe.value.ingredients?.length ? { key: 'ingredients', label: '所需食材' } : null,
    recipe.value.steps?.length ? { key: 'steps', label: '烹饪步骤' } : null,
    recipe.value.tips?.length ? { key: 'tips', label: '烹饪建议' } : null,
    recipe.value.videoKeywords?.length ? { key: 'videos', label: '视频关键词' } : null
  ].filter(Boolean)
})
const activeRecipePageIndex = computed(() => {
  if (!recipePages.value.length) {
    return 0
  }
  return Math.min(currentRecipePage.value, recipePages.value.length - 1)
})
const activeRecipePage = computed(() => recipePages.value[activeRecipePageIndex.value])

async function runSearch() {
  const normalizedIngredients = ingredients.value
    .split(/[，,、\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .join(', ')

  if (!normalizedIngredients) {
    ElMessage.warning('请至少输入一种食材')
    return
  }

  const request = {
    ingredients: normalizedIngredients,
    mealType: mealType.value,
    goal: goal.value,
    searchMode: searchMode.value
  }
  lastSearch.value = request
  recipe.value = null
  currentRecipePage.value = 0
  generating.value = true

  try {
    const response = await generateRecipe(request)
    recipe.value = response.data.data
    currentRecipePage.value = 0
    ElMessage.success('菜谱推荐已生成')
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    generating.value = false
  }
}

function resetSearch() {
  ingredients.value = ''
  mealType.value = 'any'
  goal.value = 'balanced'
  searchMode.value = 'text'
  lastSearch.value = null
  recipe.value = null
  currentRecipePage.value = 0
}

function previousRecipePage() {
  if (recipePages.value.length <= 1) {
    return
  }
  currentRecipePage.value = (activeRecipePageIndex.value - 1 + recipePages.value.length) % recipePages.value.length
}

function nextRecipePage() {
  if (recipePages.value.length <= 1) {
    return
  }
  currentRecipePage.value = (activeRecipePageIndex.value + 1) % recipePages.value.length
}

function getErrorMessage(error) {
  const message = error?.response?.data?.message || error?.message
  const messages = {
    '千问 API Key 未配置，请设置 DASHSCOPE_API_KEY': '千问 API Key 未配置，请先设置 DASHSCOPE_API_KEY',
    '千问服务调用失败，请稍后重试': '千问服务调用失败，请稍后重试',
    '千问服务未返回菜谱内容': '千问服务未返回菜谱内容',
    '千问返回内容不是有效菜谱 JSON': '千问返回内容格式异常',
    'Invalid request parameters': '请求参数不合法',
    'Network Error': '网络连接失败，请检查后端服务'
  }
  if (message?.toLowerCase().includes('timeout')) {
    return '请求超时，请稍后重试'
  }
  return messages[message] || message || '菜谱生成失败'
}
</script>

<style scoped>
.home-page {
  height: calc(100vh - 58px);
  overflow: hidden;
  padding: clamp(10px, 1.4vw, 18px);
  color: var(--app-text);
}

.command-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  width: min(1360px, 100%);
  height: 100%;
  margin: 0 auto;
}

.workspace-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0;
}

h1,
h2,
h3 {
  margin: 0;
  color: var(--app-text);
}

h1 {
  font-size: clamp(24px, 2.8vw, 38px);
  line-height: 1.02;
}

h2 {
  font-size: clamp(19px, 1.6vw, 25px);
  line-height: 1.15;
}

h3 {
  font-size: 16px;
}

.signal-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.signal-strip span,
.status-pill,
.system-tag,
.video-keywords span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  border: 1px solid var(--app-line-strong);
  border-radius: 999px;
  color: var(--app-text);
  background: var(--app-surface);
  font-size: 11px;
  font-weight: 800;
}

.command-grid {
  display: grid;
  grid-template-columns: minmax(310px, 0.78fr) minmax(0, 1.22fr);
  gap: 12px;
  min-height: 0;
}

.search-panel,
.result-panel {
  position: relative;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background:
    linear-gradient(90deg, var(--app-grid-line-strong) 1px, transparent 1px),
    linear-gradient(var(--app-grid-line-soft) 1px, transparent 1px),
    var(--app-surface);
  background-size: 28px 28px;
  box-shadow:
    var(--app-panel-shadow),
    inset 0 1px 0 var(--app-grid-line-strong);
}

.search-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  padding: clamp(12px, 1.4vw, 18px);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--app-text);
}

.panel-title > svg {
  flex: 0 0 auto;
}

.panel-title div {
  display: grid;
  gap: 2px;
}

.panel-title span,
.field-label,
.window-head span {
  color: var(--app-text-muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.panel-title strong {
  font-size: 16px;
}

.search-form {
  display: grid;
  align-content: start;
  gap: 9px;
  min-height: 0;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-form :deep(.el-select) {
  width: 100%;
}

.search-form :deep(.el-textarea__inner) {
  min-height: 86px !important;
}

.filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mode-panel {
  display: grid;
  gap: 8px;
}

.mode-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}

.mode-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  min-height: 82px;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  color: var(--app-text-muted);
  background: var(--app-surface);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms ease;
}

.mode-card:hover,
.mode-card:focus-visible,
.mode-card.active {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-surface-soft);
  outline: none;
}

.mode-card.active {
  transform: translateY(-1px);
}

.mode-card strong {
  overflow: hidden;
  color: inherit;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mode-card span {
  color: var(--app-text-faint);
  font-size: 11px;
  line-height: 1.35;
}

.search-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-top: 2px;
}

.search-actions :deep(.el-button) {
  min-height: 38px;
  margin-left: 0;
}

.search-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.result-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 9px;
  padding: clamp(12px, 1.4vw, 18px);
}

.result-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.empty-stage {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  min-height: 0;
  border: 1px dashed var(--app-line-strong);
  border-radius: 8px;
  color: var(--app-text-muted);
  text-align: center;
}

.empty-stage strong {
  color: var(--app-text);
  font-size: 18px;
}

.empty-stage span {
  width: min(420px, 92%);
  line-height: 1.7;
}

.result-content {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 9px;
  min-height: 0;
}

.brief-grid {
  display: grid;
  grid-template-columns: 1.5fr 0.7fr 0.8fr;
  gap: 8px;
  margin: 0;
}

.brief-grid div {
  min-width: 0;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.brief-grid dt {
  margin-bottom: 4px;
  color: var(--app-text-faint);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.brief-grid dd {
  margin: 0;
  overflow: hidden;
  color: var(--app-text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-detail,
.next-steps {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 9px;
  min-height: 0;
}

.recipe-summary-block {
  display: grid;
  gap: 8px;
  padding: 9px;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface-strong);
}

.recipe-summary-block p {
  display: -webkit-box;
  max-height: 44px;
  margin: 0;
  overflow: hidden;
  color: var(--app-text-soft);
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recipe-pages {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.page-toolbar {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 8px;
}

.page-arrow {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--app-line-strong);
  border-radius: 6px;
  color: var(--app-text);
  background: var(--app-surface);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.page-arrow:hover:not(:disabled),
.page-arrow:focus-visible {
  border-color: var(--app-accent);
  background: var(--app-surface-soft);
  outline: none;
}

.page-arrow:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.page-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  min-width: 0;
}

.page-tab {
  min-width: 0;
  min-height: 34px;
  padding: 0 8px;
  border: 1px solid var(--app-line);
  border-radius: 6px;
  color: var(--app-text-muted);
  background: var(--app-surface);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.page-tab:hover,
.page-tab:focus-visible,
.page-tab.active {
  border-color: var(--app-accent);
  color: var(--app-text);
  background: var(--app-surface-soft);
  outline: none;
}

.recipe-page-window,
.next-steps {
  min-height: 0;
  padding: 10px;
  overflow: auto;
  border: 1px solid var(--app-line);
  border-radius: 8px;
  background: var(--app-surface);
}

.window-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.recipe-page-window :deep(.el-table),
.next-steps :deep(.el-table) {
  overflow: hidden;
  border: 1px solid var(--app-line);
  border-radius: 8px;
}

.step-list,
.tip-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 22px;
}

.step-list li,
.tip-list li {
  color: var(--app-text-soft);
  line-height: 1.52;
}

.step-list strong {
  color: var(--app-text);
}

.step-list span {
  margin-left: 8px;
  color: var(--app-text-faint);
  font-size: 13px;
  font-weight: 800;
}

.step-list p {
  margin: 4px 0 0;
}

.video-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.video-keywords span {
  gap: 6px;
  border-radius: 6px;
}

@media (max-width: 980px) {
  .home-page {
    height: auto;
    min-height: calc(100vh - 58px);
    overflow: visible;
  }

  .command-grid {
    grid-template-columns: 1fr;
  }

  .result-panel {
    min-height: 620px;
  }
}

@media (max-width: 720px) {
  .home-page {
    min-height: calc(100vh - 121px);
    padding: 16px;
  }

  .workspace-heading {
    align-items: start;
    flex-direction: column;
  }

  .signal-strip {
    justify-content: flex-start;
  }

  .mode-grid,
  .filters,
  .brief-grid,
  .page-tabs {
    grid-template-columns: 1fr;
  }

  .search-actions {
    grid-template-columns: 1fr;
  }
}
</style>
